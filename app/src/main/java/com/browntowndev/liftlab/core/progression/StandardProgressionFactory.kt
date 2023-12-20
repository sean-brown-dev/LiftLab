package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
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
        previousResultsForDisplay: List<SetResult>,
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
                            setCount = sets.size,
                            note = workoutLift.note,
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
        loggingWorkout: LoggingWorkoutDto,
        inProgressSetResults: Map<String, SetResult>,
        microCycle: Int,
    ): LoggingWorkoutDto {
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
        workoutLift: LoggingWorkoutLiftDto,
        inProgressCompletedSets: Map<String, SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val remainingMyoRepSetResults = inProgressCompletedSets.filter { it.value is MyoRepSetResultDto }.toMutableMap()

        return workoutLift.sets.map { set ->
            val currSetKey = "${workoutLift.liftId}-${set.position}-${(set as? LoggingMyoRepSetDto)?.myoRepSetPosition}"
            val completedSet = inProgressCompletedSets[currSetKey]
            val prevCompletedSet = inProgressCompletedSets[
                "${workoutLift.liftId}-${set.position - 1}-null"
            ]

            if (completedSet != null) {
                when (set) {
                    is LoggingStandardSetDto ->
                        set.copy(
                            complete = true,
                            completedWeight = completedSet.weight,
                            completedReps = completedSet.reps,
                            completedRpe = completedSet.rpe,
                        )

                    is LoggingDropSetDto ->
                        set.copy(
                            complete = true,
                            completedWeight = completedSet.weight,
                            completedReps = completedSet.reps,
                            completedRpe = completedSet.rpe,
                        )

                    is LoggingMyoRepSetDto -> {
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
                set !is LoggingMyoRepSetDto &&
                set.weightRecommendation == null &&
                prevCompletedSet != null
            ) {
                when (set) {
                    is LoggingDropSetDto -> {
                        val increment = workoutLift.incrementOverride
                            ?: SettingsManager.getSetting(
                                INCREMENT_AMOUNT,
                                DEFAULT_INCREMENT_AMOUNT
                            )

                        val weightRecommendation = (prevCompletedSet.weight * (1 - set.dropPercentage))
                            .roundToNearestFactor(increment)

                        set.copy(weightRecommendation = weightRecommendation)
                    }

                    is LoggingStandardSetDto -> set.copy(weightRecommendation = prevCompletedSet.weight)
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
        workoutLift: LoggingWorkoutLiftDto
    ) {
        if (workoutLift.sets.none { it is LoggingMyoRepSetDto }) return

        remainingMyoRepSetResults.values
            .filterIsInstance<MyoRepSetResultDto>()
            .sortedWith(compareBy<MyoRepSetResultDto> { it.setPosition }.thenBy {
                it.myoRepSetPosition ?: -1
            })
            .forEach { myoRepResult ->
                // Try and find the logging myorep set that happened prior to this result. It should exist.
                val myoRepSetIndexPreviousToThisResult = indexOfLast {
                    it.position == myoRepResult.setPosition &&
                            (it as? LoggingMyoRepSetDto)?.myoRepSetPosition ==
                            when (myoRepResult.myoRepSetPosition) {
                                null -> -1
                                0 -> null
                                else -> myoRepResult.myoRepSetPosition - 1
                            }
                }

                if (myoRepSetIndexPreviousToThisResult > -1) {
                    val lastMyoRepSet =
                        this[myoRepSetIndexPreviousToThisResult] as? LoggingMyoRepSetDto

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
                                filterIsInstance<LoggingMyoRepSetDto>().filter { it.position == myoRepResult.setPosition }
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