package com.browntowndev.liftlab.core.domain.useCase.workout

import android.util.Log
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.isCompleteWithSameDataAs
import com.browntowndev.liftlab.core.domain.useCase.utils.MyoRepSetGoalUtils
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils

/**
 * Hydrates the lifts in a logging workout with the latest set results.
 */
class HydrateLoggingWorkoutWithCompletedSetsUseCase {
    companion object {
        private const val TAG = "HydrateLoggingWorkoutWithCompletedSetsUseCase"
    }

    private data class SetResultKey(
        val liftId: Long,
        val liftPosition: Int,
        val setPosition: Int,
        val myoRepSetPosition: Int?,
    )

    operator fun invoke(
        liftsToHydrate: List<LoggingWorkoutLift>,
        setResults: List<SetResult>,
        microCycle: Int,
    ): List<LoggingWorkoutLift> {
        return if (setResults.isNotEmpty() || liftsToHydrate.any { it.sets.any { set -> set.complete } }) {
            liftsToHydrate.fastMap { workoutLift ->
                workoutLift.copy(
                    sets =
                        getSetsWithUpdatedCompletionData(
                            workoutLift = workoutLift,
                            allSetResults = setResults,
                            isDeloadWeek = (microCycle + 1) == workoutLift.deloadWeek,
                        )
                )
            }
        } else liftsToHydrate
    }

    private fun getSetsWithUpdatedCompletionData(
        workoutLift: LoggingWorkoutLift,
        allSetResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val setResultsByKey = allSetResults.associateBy { setResult ->
            val myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
            SetResultKey(
                liftId = setResult.liftId,
                liftPosition = setResult.liftPosition,
                setPosition = setResult.setPosition,
                myoRepSetPosition = myoRepSetPosition,
            )
        }

        var lastCompletedStandardSet: GenericLoggingSet? = null

        // Using .map is still the right, functional approach.
        val updatedSets = workoutLift.sets.map { set ->
            val currSetKey = SetResultKey(
                liftId = workoutLift.liftId,
                liftPosition = workoutLift.position,
                setPosition = set.position,
                myoRepSetPosition = (set as? LoggingMyoRepSet)?.myoRepSetPosition,
            )
            val completedSetResult = setResultsByKey[currSetKey]

            val updatedSet = when {
                // Case 1: A result exists for this set.
                completedSetResult != null -> {
                    // Only copy if data is actually different.
                    if (set.isCompleteWithSameDataAs(completedSetResult)) {
                        set // Return the original object
                    } else {
                        // Data is new or different, so we must copy.
                        set.copyCompletionData(
                            complete = true,
                            completedWeight = completedSetResult.weight,
                            completedReps = completedSetResult.reps,
                            completedRpe = completedSetResult.rpe,
                        )
                    }
                }

                // Case 2: No set result, but it's currently marked as complete.
                // This means it needs to be "un-completed".
                set.complete -> {
                    set.copyCompletionData(
                        complete = false,
                        completedWeight = set.completedWeight, // Keep old data, more convenient
                        completedReps = set.completedReps,
                        completedRpe = set.completedRpe,
                    )
                }

                // Case 3: No result, not complete, and previous is complete, update weight recommendation
                set !is LoggingMyoRepSet && lastCompletedStandardSet?.position == set.position - 1 -> {
                    getWithWeightRecommendation(
                        workoutLift = workoutLift,
                        set = set,
                        lastCompletedStandardSet = lastCompletedStandardSet
                    )
                }

                // Case 4: None of the above apply, return the set as-is.
                else -> set
            }

            // Update the last completed set for the next iteration.
            lastCompletedStandardSet = if (updatedSet.complete && (updatedSet is LoggingStandardSet ||
                        (updatedSet is LoggingMyoRepSet && updatedSet.myoRepSetPosition == null))) {
                updatedSet
            } else {
                lastCompletedStandardSet
            }

            updatedSet
        }

        // Add new myo reps if needed
        return updatedSets.toMutableList().apply {
            addNextMyoRepsToComplete(isDeloadWeek, workoutLift)
        }
    }

    private fun getWithWeightRecommendation(
        workoutLift: LoggingWorkoutLift,
        set: GenericLoggingSet,
        lastCompletedStandardSet: GenericLoggingSet,
    ): GenericLoggingSet = when (set) {
        is LoggingDropSet -> {
            val increment = workoutLift.incrementOverride
                ?: SettingsManager.getSetting(
                    SettingsManager.SettingNames.INCREMENT_AMOUNT,
                    SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
                )

            val weightRecommendation = (lastCompletedStandardSet.completedWeight!! * (1 - set.dropPercentage))
                .roundToNearestFactor(increment)
            set.copy(weightRecommendation = weightRecommendation)
        }

        is LoggingStandardSet -> {
            val lastMissedGoal = lastCompletedStandardSet.completedReps!! < lastCompletedStandardSet.repRangeBottom!! ||
                    lastCompletedStandardSet.completedRpe!! > lastCompletedStandardSet.rpeTarget
            val lastGoalDiffered = lastCompletedStandardSet.repRangeBottom != set.repRangeBottom ||
                    lastCompletedStandardSet.rpeTarget != set.rpeTarget

            val weightRecommendation = if (lastMissedGoal || lastGoalDiffered) {
                WeightCalculationUtils.calculateSuggestedWeight(
                    completedWeight = lastCompletedStandardSet.completedWeight!!,
                    completedReps = lastCompletedStandardSet.completedReps!!,
                    completedRpe = lastCompletedStandardSet.completedRpe!!,
                    repGoal = set.repRangeBottom,
                    rpeGoal = set.rpeTarget,
                    roundingFactor = workoutLift.incrementOverride ?: SettingsManager.getSetting(
                        SettingsManager.SettingNames.INCREMENT_AMOUNT,
                        SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT,
                    ),
                )
            }
            else set.weightRecommendation ?: lastCompletedStandardSet.completedWeight
            set.copy(weightRecommendation = weightRecommendation)
        }
        else -> throw Exception("${set::class.simpleName} is not defined.")
    }

    private fun MutableList<GenericLoggingSet>.addNextMyoRepsToComplete(
        isDeloadWeek: Boolean,
        workoutLift: LoggingWorkoutLift
    ) {
        val myoRepSets = this.filterIsInstance<LoggingMyoRepSet>()
        Log.d(TAG, "myoRepSets: $myoRepSets")
        if (myoRepSets.isEmpty()) return

        myoRepSets
            .sortedBy { it.myoRepSetPosition }
            .groupBy { it.position }
            .values
            .forEach { myoRepResults ->
                val lastMyoRepSet = myoRepResults.last()
                val result = MyoRepSetGoalUtils.shouldContinueMyoReps(
                    lastMyoRepSet = lastMyoRepSet,
                    myoRepSetResults = myoRepResults,
                )

                if (result.shouldContinueMyoReps) {
                    val myoRepSetPosition = (lastMyoRepSet.myoRepSetPosition ?: -1) + 1
                    val weightRecommendation = if (!result.activationSetMissedGoal) {
                        lastMyoRepSet.completedWeight
                    } else WeightCalculationUtils.Companion.calculateSuggestedWeight(
                        completedWeight = lastMyoRepSet.completedWeight!!,
                        completedReps = lastMyoRepSet.completedReps!!,
                        completedRpe = lastMyoRepSet.completedRpe!!,
                        repGoal = lastMyoRepSet.repRangeBottom!!,
                        rpeGoal = lastMyoRepSet.rpeTarget,
                        roundingFactor = workoutLift.incrementOverride
                            ?: SettingsManager.getSetting(
                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT,
                            ),
                    )

                    val newMyoRepSet = lastMyoRepSet.copy(
                        myoRepSetPosition = myoRepSetPosition,
                        repRangePlaceholder = if (!isDeloadWeek && lastMyoRepSet.repFloor != null) {
                            ">${lastMyoRepSet.repFloor}"
                        } else if (!isDeloadWeek) {
                            "—"
                        } else {
                            lastMyoRepSet.repRangeBottom.toString()
                        },
                        weightRecommendation = weightRecommendation,
                        complete = false,
                        completedWeight = null,
                        completedReps = null,
                        completedRpe = null,
                    )
                    Log.d(TAG, "newMyoRepSet: $newMyoRepSet")
                    add(
                        index = this.indexOf(lastMyoRepSet) + 1,
                        element = newMyoRepSet
                    )
                }
            }
    }
}