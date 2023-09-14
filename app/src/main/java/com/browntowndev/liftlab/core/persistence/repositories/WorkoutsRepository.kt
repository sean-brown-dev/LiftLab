package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.SettingsManager
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
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.progression.MyoRepSetGoalValidator
import com.browntowndev.liftlab.core.progression.ProgressionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class WorkoutsRepository(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val workoutMapper: WorkoutMapper,
    private val workoutsDao: WorkoutsDao,
    private val progressionFactory: ProgressionFactory,
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        workoutsDao.updateName(id, newName)
    }

    suspend fun get(id: Long): WorkoutDto {
        return workoutMapper.map(workoutsDao.get(id))
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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getNextToPerform(
        programMetadata: ActiveProgramMetadataDto,
    ): LiveData<LoggingWorkoutDto?> {
        return workoutsDao.getByMicrocyclePosition(programMetadata.currentMicrocyclePosition)
            .flatMapLatest { workout ->
                val previousSetResults = if (workout != null) {
                    previousSetResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicro(
                        workoutId = workout.workout.id,
                        mesoCycle = programMetadata.currentMesocycle,
                        microCycle = programMetadata.currentMicrocycle,
                    )
                } else listOf()

                val loggingWorkout = if (workout!= null) {
                    progressionFactory.calculate(
                        workout = workoutMapper.map(workout),
                        previousSetResults = previousSetResults,
                        programDeloadWeek = programMetadata.deloadWeek,
                        microCycle = programMetadata.currentMicrocycle,
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
            }.asLiveData()
    }

    private fun copyInProgressSet(
        inProgressCompletedSets: Map<String, SetResult>,
        workoutLift: LoggingWorkoutLiftDto,
        set: GenericLoggingSet,
        index: Int,
        programMetadata: ActiveProgramMetadataDto
    ): List<GenericLoggingSet> {
        val completedSet = inProgressCompletedSets[
            "${workoutLift.liftId}-${set.setPosition}-${(set as? LoggingMyoRepSetDto)?.myoRepSetPosition}"
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
        } else if (set is LoggingDropSetDto) {
            val prevCompletedSet = inProgressCompletedSets[
                "${workoutLift.liftId}-${set.setPosition - 1}-null"
            ]

            listOf(
                if (prevCompletedSet != null) {
                    val increment = workoutLift.incrementOverride
                        ?: workoutLift.liftIncrementOverride
                        ?: SettingsManager.getSetting(
                            SettingsManager.SettingNames.INCREMENT_AMOUNT,
                            5f
                        )

                    val weightRecommendation = (prevCompletedSet.weight * (1 - set.dropPercentage))
                        .roundToNearestFactor(increment)

                    set.copy(weightRecommendation = weightRecommendation)
                } else set
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
        val isLast = !hasMoreSets || (nextSet!!.setPosition != set.setPosition)
        var nextInProgressSetResult = inProgressCompletedSets[
            "${workoutLift.liftId}-${set.setPosition}-${(set.myoRepSetPosition ?: -1) + 1}"
        ] as? MyoRepSetResultDto

        while (isLast && nextInProgressSetResult != null) {
            val myoRepSetPosition = nextInProgressSetResult.myoRepSetPosition!!

            val isDeloadWeek =
                (programMetadata.currentMicrocycle + 1) ==
                        (workoutLift.deloadWeek
                            ?: programMetadata.deloadWeek)

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
                    "${workoutLift.liftId}-${set.setPosition}-${(nextInProgressSetResult.myoRepSetPosition ?: -1) + 1}"
                ] as? MyoRepSetResultDto

            if (nextInProgressSetResult == null &&
                MyoRepSetGoalValidator.validate(
                    myoRepSetGoals = set,
                    completedMyoRepSetResult = lastCompletedSet,
                    previousMyoRepSets = myoRepSets,
                )
            ) {
                myoRepSets.add(
                    LoggingMyoRepSetDto(
                        setPosition = set.setPosition,
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
                    )
                )
            }
        }

        return myoRepSets
    }
}