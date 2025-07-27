package com.browntowndev.liftlab.core.domain.useCase.workout

import android.util.Log
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.domain.models.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.utils.MyoRepSetGoalUtils
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils

class HydrateLoggingWorkoutWithCompletedSetsUseCase {
    companion object {
        private const val TAG = "HydrateLoggingWorkoutWithCompletedSetsUseCase"
    }

    fun hydrateWithInProgressSetResults(
        loggingWorkout: LoggingWorkout,
        inProgressSetResults: List<SetResult>,
        microCycle: Int,
    ): LoggingWorkout {
        return if (inProgressSetResults.isNotEmpty() || loggingWorkout.lifts.any { it.sets.any { set -> set.complete } }) {
            val updatedLifts = loggingWorkout.lifts
                .fastMap { workoutLift ->
                    workoutLift.copy(sets =
                        getSetsWithUpdatedCompletionData(
                            workoutLift = workoutLift,
                            inProgressCompletedSets = inProgressSetResults.filter { it.liftPosition == workoutLift.position },
                            isDeloadWeek = (microCycle + 1) == workoutLift.deloadWeek,
                        )
                    )
                }

            loggingWorkout.copy(lifts = updatedLifts)
        } else loggingWorkout
    }

    private fun getSetsWithUpdatedCompletionData(
        workoutLift: LoggingWorkoutLift,
        inProgressCompletedSets: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val setResultsByKey = inProgressCompletedSets.associateBy { setResult ->
            val myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
            "${workoutLift.liftId}-${setResult.setPosition}-$myoRepSetPosition"
        }
        var lastCompletedStandardSet: GenericLoggingSet? = null
        return workoutLift.sets.map { set ->
            val currSetKey = "${workoutLift.liftId}-${set.position}-${(set as? LoggingMyoRepSet)?.myoRepSetPosition}"
            val completedSetResult = setResultsByKey[currSetKey]

            val updatedSet = (if (completedSetResult != null) {
                set.copyCompletionData(
                    complete = true,
                    completedWeight = completedSetResult.weight,
                    completedReps = completedSetResult.reps,
                    completedRpe = completedSetResult.rpe,
                )
            } else if (
                set !is LoggingMyoRepSet &&
                set.weightRecommendation == null &&
                lastCompletedStandardSet != null
            ) {
                getWithWeightRecommendation(
                    workoutLift = workoutLift,
                    set = set,
                    lastCompletedStandardSet = lastCompletedStandardSet)
            } else set).let { setToCheckForIncompletion ->
                Log.d(TAG, "setToCheckForIncompletion: $setToCheckForIncompletion")
                if (completedSetResult == null && setToCheckForIncompletion.complete) {
                    setToCheckForIncompletion.copyCompletionData(
                        complete = false,
                        completedWeight = null,
                        completedReps = null,
                        completedRpe = null,
                    )
                } else setToCheckForIncompletion
            }

            lastCompletedStandardSet = if (updatedSet.complete && updatedSet is LoggingStandardSet ||
                updatedSet is LoggingMyoRepSet && updatedSet.myoRepSetPosition == null) updatedSet
            else lastCompletedStandardSet
            Log.d(TAG, "updatedSet: $updatedSet")
            updatedSet
        }.toMutableList().apply {
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
            val weightRecommendation = if (
                lastCompletedStandardSet.completedReps!! < lastCompletedStandardSet.repRangeBottom!! &&
                lastCompletedStandardSet.completedRpe!! > lastCompletedStandardSet.rpeTarget
            ) {
                WeightCalculationUtils.Companion.calculateSuggestedWeight(
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
            else lastCompletedStandardSet.weightRecommendation
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