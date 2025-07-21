package com.browntowndev.liftlab.core.domain.progression

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.domain.models.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult

class StandardProgressionFactory: ProgressionFactory {
    override fun calculate(
        workout: Workout,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        inProgressSetResults: Map<String, SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
        useLiftSpecificDeloading: Boolean,
        onlyUseResultsForLiftsInSamePosition: Boolean,
    ): LoggingWorkout {
        var loggingWorkout = LoggingWorkout(
            id = workout.id,
            name = workout.name,
            lifts = listOf()
        )

        workout.lifts.fastForEach { workoutLift ->
            val deloadWeek = if (useLiftSpecificDeloading) workoutLift.deloadWeek else programDeloadWeek
            val isDeloadWeek = (microCycle + 1) == deloadWeek
            val resultsForLift = previousSetResults.filter { result ->
                (!onlyUseResultsForLiftsInSamePosition || result.liftPosition == workoutLift.position)
                        && result.liftId == workoutLift.liftId
            }
            val displayResultsForLift = previousResultsForDisplay.filter { result ->
                (!onlyUseResultsForLiftsInSamePosition || result.liftPosition == workoutLift.position)
                        && result.liftId == workoutLift.liftId
            }

            loggingWorkout = loggingWorkout.copy(
                lifts = loggingWorkout.lifts.toMutableList().apply {
                    val sets = when (workoutLift.progressionScheme) {
                        ProgressionScheme.DOUBLE_PROGRESSION -> DoubleProgressionCalculator()
                        ProgressionScheme.LINEAR_PROGRESSION -> LinearProgressionCalculator()
                        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> DynamicDoubleProgressionCalculator()
                        ProgressionScheme.WAVE_LOADING_PROGRESSION -> WaveLoadingProgressionCalculator(programDeloadWeek, microCycle)
                    }.calculate(
                        workoutLift = workoutLift,
                        previousSetResults = resultsForLift,
                        previousResultsForDisplay = displayResultsForLift,
                        isDeloadWeek = isDeloadWeek,
                    )
                    
                    add(
                        LoggingWorkoutLift(
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
                            setCount = sets.size,
                            note = workoutLift.liftNote,
                            sets = sets,
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
        loggingWorkout: LoggingWorkout,
        inProgressSetResults: Map<String, SetResult>,
        microCycle: Int,
    ): LoggingWorkout {
        return if (inProgressSetResults.isNotEmpty()) {
            val workoutWithInProgressResults = loggingWorkout.copy(
                lifts = loggingWorkout.lifts.fastMap { workoutLift ->
                    workoutLift.copy(
                        sets = copyInProgressSets(
                            workoutLift = workoutLift,
                            inProgressCompletedSets = inProgressSetResults,
                            isDeloadWeek = (microCycle + 1) == workoutLift.deloadWeek,
                        )
                    )
                }
            )
            workoutWithInProgressResults
        } else loggingWorkout
    }

    private fun copyInProgressSets(
        workoutLift: LoggingWorkoutLift,
        inProgressCompletedSets: Map<String, SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val remainingMyoRepSetResults = inProgressCompletedSets.filter { it.value is MyoRepSetResult }.toMutableMap()

        return workoutLift.sets.map { set ->
            val currSetKey = "${workoutLift.liftId}-${set.position}-${(set as? LoggingMyoRepSet)?.myoRepSetPosition}"
            val completedSet = inProgressCompletedSets[currSetKey]
            val prevCompletedSet = inProgressCompletedSets[
                "${workoutLift.liftId}-${set.position - 1}-null"
            ]

            if (completedSet != null) {
                when (set) {
                    is LoggingStandardSet ->
                        set.copy(
                            complete = true,
                            completedWeight = completedSet.weight,
                            completedReps = completedSet.reps,
                            completedRpe = completedSet.rpe,
                        )

                    is LoggingDropSet ->
                        set.copy(
                            complete = true,
                            completedWeight = completedSet.weight,
                            completedReps = completedSet.reps,
                            completedRpe = completedSet.rpe,
                        )

                    is LoggingMyoRepSet -> {
                        remainingMyoRepSetResults.remove(currSetKey)

                        set.copy(
                            complete = true,
                            completedWeight = completedSet.weight,
                            completedReps = completedSet.reps,
                            completedRpe = completedSet.rpe,
                        )
                    }

                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            } else if (
                set !is LoggingMyoRepSet &&
                set.weightRecommendation == null &&
                prevCompletedSet != null
            ) {
                when (set) {
                    is LoggingDropSet -> {
                        val increment = workoutLift.incrementOverride
                            ?: SettingsManager.getSetting(
                                INCREMENT_AMOUNT,
                                DEFAULT_INCREMENT_AMOUNT
                            )

                        val weightRecommendation = (prevCompletedSet.weight * (1 - set.dropPercentage))
                            .roundToNearestFactor(increment)

                        set.copy(weightRecommendation = weightRecommendation)
                    }

                    is LoggingStandardSet -> set.copy(weightRecommendation = prevCompletedSet.weight)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            } else set
        }.toMutableList().apply {
            addMissingMyorepSetsFromInProgressResults(remainingMyoRepSetResults, isDeloadWeek, workoutLift)
        }
    }

    private fun MutableList<GenericLoggingSet>.addMissingMyorepSetsFromInProgressResults(
        remainingMyoRepSetResults: MutableMap<String, SetResult>,
        isDeloadWeek: Boolean,
        workoutLift: LoggingWorkoutLift
    ) {
        if (workoutLift.sets.none { it is LoggingMyoRepSet }) return

        remainingMyoRepSetResults.values
            .filterIsInstance<MyoRepSetResult>()
            .sortedWith(compareBy<MyoRepSetResult> { it.setPosition }.thenBy {
                it.myoRepSetPosition ?: -1
            })
            .forEach { myoRepResult ->
                // Try and find the logging myorep set that happened prior to this result. It should exist.
                val myoRepSetIndexPreviousToThisResult = indexOfLast {
                    it.position == myoRepResult.setPosition &&
                            (it as? LoggingMyoRepSet)?.myoRepSetPosition ==
                            when (myoRepResult.myoRepSetPosition) {
                                null -> -1
                                0 -> null
                                else -> myoRepResult.myoRepSetPosition - 1
                            }
                }

                if (myoRepSetIndexPreviousToThisResult > -1) {
                    val lastMyoRepSet =
                        this[myoRepSetIndexPreviousToThisResult] as? LoggingMyoRepSet

                    if (lastMyoRepSet != null) {
                        add(
                            index = myoRepSetIndexPreviousToThisResult + 1,
                            lastMyoRepSet.copy(
                                myoRepSetPosition = myoRepResult.myoRepSetPosition,
                                repRangePlaceholder = if (!isDeloadWeek && lastMyoRepSet.repFloor != null) {
                                    ">${lastMyoRepSet.repFloor}"
                                } else if (!isDeloadWeek) {
                                    "—"
                                } else {
                                    lastMyoRepSet.repRangeBottom.toString()
                                },
                                complete = true,
                                completedWeight = myoRepResult.weight,
                                completedReps = myoRepResult.reps,
                                completedRpe = myoRepResult.rpe,
                            )
                        )

                        // If this was the last myorep for this set position add another one if need be
                        val nextMyoRepSetPosition = (myoRepResult.myoRepSetPosition ?: -1) + 1
                        if (!remainingMyoRepSetResults.containsKey("${workoutLift.liftId}-${myoRepResult.setPosition}-$nextMyoRepSetPosition")) {
                            val myoRepResults =
                                filterIsInstance<LoggingMyoRepSet>().filter { it.position == myoRepResult.setPosition }
                            MyoRepSetGoalValidator.shouldContinueMyoReps(
                                completedSet = lastMyoRepSet,
                                myoRepSetResults = myoRepResults,
                            ).let { continueMyoReps ->
                                if (continueMyoReps) {
                                    add(
                                        index = myoRepSetIndexPreviousToThisResult + 2,
                                        lastMyoRepSet.copy(
                                            myoRepSetPosition = nextMyoRepSetPosition,
                                            repRangePlaceholder = if (!isDeloadWeek && lastMyoRepSet.repFloor != null) {
                                                ">${lastMyoRepSet.repFloor}"
                                            } else if (!isDeloadWeek) {
                                                "—"
                                            } else {
                                                lastMyoRepSet.repRangeBottom.toString()
                                            },
                                            weightRecommendation = lastMyoRepSet.completedWeight,
                                            complete = false,
                                            completedWeight = null,
                                            completedReps = null,
                                            completedRpe = null,
                                        )
                                    )
                                }

                                if (!continueMyoReps &&
                                    MyoRepSetGoalValidator.shouldContinueMyoReps(
                                        completedSet = lastMyoRepSet,
                                        myoRepSetResults = myoRepResults,
                                        activationSetAlwaysSuccess = true,
                                    )
                                ) {
                                    add(
                                        index = myoRepSetIndexPreviousToThisResult + 2,
                                        lastMyoRepSet.copy(
                                            myoRepSetPosition = nextMyoRepSetPosition,
                                            repRangePlaceholder = if (!isDeloadWeek && lastMyoRepSet.repFloor != null) {
                                                ">${lastMyoRepSet.repFloor}"
                                            } else if (!isDeloadWeek) {
                                                "—"
                                            } else {
                                                lastMyoRepSet.repRangeBottom.toString()
                                            },
                                            weightRecommendation = CalculationEngine.calculateSuggestedWeight(
                                                completedWeight = lastMyoRepSet.completedWeight!!,
                                                completedReps = lastMyoRepSet.completedReps!!,
                                                completedRpe = lastMyoRepSet.completedRpe!!,
                                                repGoal = lastMyoRepSet.repRangeBottom!!,
                                                rpeGoal = lastMyoRepSet.rpeTarget,
                                                roundingFactor = workoutLift.incrementOverride
                                                    ?: SettingsManager.getSetting(
                                                        INCREMENT_AMOUNT,
                                                        DEFAULT_INCREMENT_AMOUNT,
                                                    ),
                                            ),
                                            complete = false,
                                            completedWeight = null,
                                            completedReps = null,
                                            completedRpe = null,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}