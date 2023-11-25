package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class StandardProgressionFactory: ProgressionFactory {
    override fun calculate(
        workout: WorkoutDto,
        previousSetResults: List<SetResult>,
        inProgressSetResults: Map<String, SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
        onlyUseResultsForLiftsInSamePosition: Boolean,
    ): LoggingWorkoutDto {
        var loggingWorkout = LoggingWorkoutDto(
            id = workout.id,
            name = workout.name,
            lifts = listOf()
        )

        workout.lifts.fastForEach { workoutLift ->
            val deloadWeek = workoutLift.deloadWeek ?: programDeloadWeek
            val isDeloadWeek = (microCycle + 1) == deloadWeek
            val resultsForLift = previousSetResults.filter { result ->
                (!onlyUseResultsForLiftsInSamePosition || result.liftPosition == workoutLift.position)
                        && result.liftId == workoutLift.liftId
            }

            loggingWorkout = loggingWorkout.copy(
                lifts = loggingWorkout.lifts.toMutableList().apply {
                    add(
                        LoggingWorkoutLiftDto(
                            id = workoutLift.id,
                            liftId = workoutLift.liftId,
                            liftName = workoutLift.liftName,
                            liftMovementPattern = workoutLift.liftMovementPattern,
                            liftVolumeTypes = workoutLift.liftVolumeTypes,
                            liftSecondaryVolumeTypes = workoutLift.liftSecondaryVolumeTypes,
                            deloadWeek = deloadWeek,
                            incrementOverride = workoutLift.incrementOverride,
                            position = workoutLift.position,
                            progressionScheme = workoutLift.progressionScheme,
                            restTime = workoutLift.restTime,
                            restTimerEnabled = workoutLift.restTimerEnabled,
                            setCount = workoutLift.setCount,
                            sets = when (workoutLift.progressionScheme) {
                                ProgressionScheme.DOUBLE_PROGRESSION -> DoubleProgressionCalculator()
                                ProgressionScheme.LINEAR_PROGRESSION -> LinearProgressionCalculator()
                                ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> DynamicDoubleProgressionCalculator()
                                ProgressionScheme.WAVE_LOADING_PROGRESSION -> WaveLoadingProgressionCalculator(programDeloadWeek, microCycle)
                            }.calculate(
                                workoutLift = workoutLift,
                                previousSetResults = resultsForLift,
                                isDeloadWeek = isDeloadWeek,
                            )
                        )
                    )
                }
            )
        }

        return updateWithInProgressSetResults(
            loggingWorkout = loggingWorkout,
            inProgressSetResults = inProgressSetResults,
            microCycle = microCycle,
        )
    }

    private fun updateWithInProgressSetResults(
        loggingWorkout: LoggingWorkoutDto,
        inProgressSetResults: Map<String, SetResult>,
        microCycle: Int,
    ): LoggingWorkoutDto {
        return if (inProgressSetResults.isNotEmpty()) {
            loggingWorkout.copy(
                lifts = loggingWorkout.lifts.fastMap { workoutLift ->
                    workoutLift.copy(
                        sets = workoutLift.sets.flatMapIndexed { index, set ->
                            copyInProgressSet(
                                inProgressSetResults,
                                workoutLift,
                                set,
                                index,
                                microCycle
                            )
                        }
                    )
                }
            )
        } else loggingWorkout
    }

    private fun copyInProgressSet(
        inProgressCompletedSets: Map<String, SetResult>,
        workoutLift: LoggingWorkoutLiftDto,
        set: GenericLoggingSet,
        index: Int,
        microCycle: Int,
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
                        microCycle,
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
                                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
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
        microCycle: Int,
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
            val isDeloadWeek = (microCycle + 1) == workoutLift.deloadWeek

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