package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.progression.MyoRepSetGoalValidator
import com.browntowndev.liftlab.core.progression.ProgressionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class WorkoutsRepository(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val loggingRepository: LoggingRepository,
    private val workoutMapper: WorkoutMapper,
    private val setResultMapper: SetResultMapper,
    private val workoutsDao: WorkoutsDao,
    private val progressionFactory: ProgressionFactory,
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        workoutsDao.updateName(id, newName)
    }

    suspend fun insert(workout: WorkoutDto): Long {
        return workoutsDao.insert(workout = workoutMapper.map(workout))
    }

    suspend fun delete(workout: WorkoutDto) {
        workoutsDao.delete(workoutMapper.map(workout))
    }

    suspend fun updateMany(workouts: List<WorkoutDto>) {
        workoutsDao.updateMany(workouts.map { workoutMapper.map(it) })
    }

    suspend fun update(workout: WorkoutDto) {
        val updWorkout = workoutMapper.map(workout)
        val updSets = workout.lifts
            .filterIsInstance<CustomWorkoutLiftDto>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
        workoutLiftsRepository.updateMany(workout.lifts)
        customLiftSetsRepository.updateMany(updSets)
    }

    suspend fun get(workoutId: Long): WorkoutDto? {
        return workoutsDao.get(workoutId)?.let {
             workoutMapper.map(it)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getNextToPerform(
        programMetadata: ActiveProgramMetadataDto,
    ): LiveData<LoggingWorkoutDto?> {
        return workoutsDao.getByMicrocyclePosition(
            programId = programMetadata.programId,
            microcyclePosition = programMetadata.currentMicrocyclePosition
        ).flatMapLatest { workout ->
            getSetResults(workout, programMetadata)
                .flatMapLatest { previousSetResults ->
                    SettingsManager.getSettingFlow(
                        ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
                        DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
                    ).flatMapLatest { onlyUseResultsForLiftsInSamePosition ->
                        val loggingWorkout = if (workout != null) {
                            progressionFactory.calculate(
                                workout = workoutMapper.map(workout),
                                previousSetResults = previousSetResults,
                                programDeloadWeek = programMetadata.deloadWeek,
                                microCycle = programMetadata.currentMicrocycle,
                                onlyUseResultsForLiftsInSamePosition = onlyUseResultsForLiftsInSamePosition,
                            )
                        } else null

                        val inProgressCompletedSets = if (workout != null) {
                            previousSetResultsRepository.getForWorkout(
                                workoutId = workout.workout.id,
                                mesoCycle = programMetadata.currentMesocycle,
                                microCycle = programMetadata.currentMicrocycle
                            ).associateBy { result ->
                                "${result.liftId}-${result.setPosition}-${(result as? MyoRepSetResultDto)?.myoRepSetPosition}"
                            }
                        } else mapOf()

                        flowOf(
                            if (inProgressCompletedSets.isNotEmpty() && loggingWorkout != null) {
                                loggingWorkout.copy(
                                    lifts = loggingWorkout.lifts.fastMap { workoutLift ->
                                        workoutLift.copy(
                                            sets = workoutLift.sets.flatMapIndexed { index, set ->
                                                copyInProgressSet(
                                                    inProgressCompletedSets,
                                                    workoutLift,
                                                    set,
                                                    index,
                                                    programMetadata
                                                )
                                            }
                                        )
                                    }
                                )
                            } else loggingWorkout
                        )
                    }
                }
        }.asLiveData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getSetResults(
        workout: WorkoutWithRelationships?,
        programMetadata: ActiveProgramMetadataDto
    ): Flow<List<SetResult>> {
        return SettingsManager.getSettingFlow(
            USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
            DEFAULT_USE_ALL_WORKOUT_DATA
        ).flatMapLatest { useAllData ->
            flowOf(
                if (workout != null) {
                    val resultsFromLastWorkout =
                        previousSetResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicro(
                            workoutId = workout.workout.id,
                            mesoCycle = programMetadata.currentMesocycle,
                            microCycle = programMetadata.currentMicrocycle,
                        )

                    if (useAllData) {
                        getResultsWithAllWorkoutDataAppended(resultsFromLastWorkout, workout)
                    } else resultsFromLastWorkout
                } else listOf()
            )
        }
    }

    private suspend fun getResultsWithAllWorkoutDataAppended(
        resultsFromLastWorkout: List<SetResult>,
        workout: WorkoutWithRelationships
    ): List<SetResult> {
        val liftIdsOfResults = resultsFromLastWorkout.map { it.liftId }.toHashSet()
        val liftIdsToSearchFor = workout.lifts
            .filter { !liftIdsOfResults.contains(it.lift.id) }
            .map { workoutLift -> workoutLift.lift.id }

        return if (liftIdsToSearchFor.isNotEmpty()) {
            val linearProgressionLiftIds = workout.lifts
                .filter {
                    it.workoutLift.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION
                }.map { it.lift.id }
                .toHashSet()

            resultsFromLastWorkout.toMutableList().apply {
                val resultsFromOtherWorkouts =
                    loggingRepository.getMostRecentLogsForLiftIds(liftIdsToSearchFor)
                        .flatMap { workoutLog ->
                            workoutLog.setResults.fastMap { setLogEntry ->
                                setResultMapper.map(
                                    from = setLogEntry,
                                    workoutId = workout.workout.id,
                                    isLinearProgression = linearProgressionLiftIds.contains(
                                        setLogEntry.liftId
                                    )
                                )
                            }
                        }
                addAll(resultsFromOtherWorkouts)
            }
        } else resultsFromLastWorkout
    }

    private fun copyInProgressSet(
        inProgressCompletedSets: Map<String, SetResult>,
        workoutLift: LoggingWorkoutLiftDto,
        set: GenericLoggingSet,
        index: Int,
        programMetadata: ActiveProgramMetadataDto
    ): List<GenericLoggingSet> {
        val completedSet = inProgressCompletedSets[
            "${workoutLift.liftId}-${set.position}-${(set as? LoggingMyoRepSetDto)?.myoRepSetPosition}"
        ]
        val prevCompletedSet = inProgressCompletedSets[
            "${workoutLift.liftId}-${set.position - 1}-null"
        ]

        return if (completedSet != null) {
            when (set) {
                is LoggingStandardSetDto -> listOf(
                    set.copy(
                        complete = true,
                        completedWeight = completedSet.weight,
                        completedReps = completedSet.reps,
                        completedRpe = completedSet.rpe,
                    )
                )

                is LoggingDropSetDto -> listOf(
                    set.copy(
                        complete = true,
                        completedWeight = completedSet.weight,
                        completedReps = completedSet.reps,
                        completedRpe = completedSet.rpe,
                    )
                )

                is LoggingMyoRepSetDto -> {
                    copyInProgressMyoRepSets(
                        set,
                        completedSet,
                        index,
                        workoutLift,
                        inProgressCompletedSets,
                        programMetadata
                    )
                }

                else -> throw Exception("${set::class.simpleName} is not defined.")
            }
        } else if (
            set !is LoggingMyoRepSetDto &&
            set.weightRecommendation == null &&
            prevCompletedSet != null
        ) {
            listOf(
                when (set) {
                    is LoggingDropSetDto -> {
                        val increment = workoutLift.incrementOverride
                            ?: SettingsManager.getSetting(
                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                DEFAULT_INCREMENT_AMOUNT
                            )

                        val weightRecommendation = (prevCompletedSet.weight * (1 - set.dropPercentage))
                            .roundToNearestFactor(increment)

                        set.copy(weightRecommendation = weightRecommendation)
                    }
                    is LoggingStandardSetDto -> set.copy(weightRecommendation = prevCompletedSet.weight)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            )
        } else listOf(set)
    }

    private fun copyInProgressMyoRepSets(
        set: LoggingMyoRepSetDto,
        completedSet: SetResult,
        index: Int,
        workoutLift: LoggingWorkoutLiftDto,
        inProgressCompletedSets: Map<String, SetResult>,
        programMetadata: ActiveProgramMetadataDto
    ): MutableList<LoggingMyoRepSetDto> {
        val myoRepSets = mutableListOf(
            set.copy(
                complete = true,
                completedWeight = completedSet.weight,
                completedReps = completedSet.reps,
                completedRpe = completedSet.rpe,
            )
        )

        val hasMoreSets = index < (workoutLift.sets.size - 1)
        val nextSet = if (hasMoreSets) workoutLift.sets[index + 1] else null
        val isLast = !hasMoreSets || (nextSet!!.position != set.position)
        var nextInProgressSetResult = inProgressCompletedSets[
            "${workoutLift.liftId}-${set.position}-${(set.myoRepSetPosition ?: -1) + 1}"
        ] as? MyoRepSetResultDto

        while (isLast && nextInProgressSetResult != null) {
            val myoRepSetPosition = nextInProgressSetResult.myoRepSetPosition!!
            val isDeloadWeek = (programMetadata.currentMicrocycle + 1) == workoutLift.deloadWeek

            myoRepSets.add(
                set.copy(
                    complete = true,
                    myoRepSetPosition = myoRepSetPosition,
                    repRangePlaceholder = if (!isDeloadWeek && set.repFloor != null) {
                        ">${set.repFloor}"
                    } else if (!isDeloadWeek) {
                        "—"
                    } else {
                        set.repRangeBottom.toString()
                    },
                    completedWeight = nextInProgressSetResult.weight,
                    completedReps = nextInProgressSetResult.reps,
                    completedRpe = nextInProgressSetResult.rpe,
                )
            )

            val lastCompletedSet = nextInProgressSetResult.copy()
            nextInProgressSetResult =
                inProgressCompletedSets[
                    "${workoutLift.liftId}-${set.position}-${(nextInProgressSetResult.myoRepSetPosition ?: -1) + 1}"
                ] as? MyoRepSetResultDto

            if (nextInProgressSetResult == null &&
                MyoRepSetGoalValidator.shouldContinueMyoReps(
                    completedRpe = lastCompletedSet.rpe,
                    completedReps = lastCompletedSet.reps,
                    myoRepSetGoals = set,
                    previousMyoRepSets = myoRepSets,
                )
            ) {
                myoRepSets.add(
                    LoggingMyoRepSetDto(
                        position = set.position,
                        myoRepSetPosition = myoRepSetPosition + 1,
                        rpeTarget = set.rpeTarget,
                        repRangeBottom = set.repRangeBottom,
                        repRangeTop = set.repRangeTop,
                        setMatching = set.setMatching,
                        maxSets = set.maxSets,
                        repFloor = set.repFloor,
                        previousSetResultLabel = "—",
                        repRangePlaceholder = if (!isDeloadWeek && set.repFloor != null) {
                            ">${set.repFloor}"
                        } else if (!isDeloadWeek) {
                            "—"
                        } else {
                            set.repRangeBottom.toString()
                        },
                        weightRecommendation = completedSet.weight,
                        hadInitialWeightRecommendation = true,
                    )
                )
            }
        }

        return myoRepSets
    }
}