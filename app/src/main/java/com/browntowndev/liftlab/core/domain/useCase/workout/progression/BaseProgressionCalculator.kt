package com.browntowndev.liftlab.core.domain.useCase.workout.progression

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationDropSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils

abstract class BaseProgressionCalculator: ProgressionCalculator {
    protected fun getDropSetRecommendation(
        lift: CalculationWorkoutLift,
        set: CalculationDropSet,
        previousSetWeight: Float?,
    ): Float? {
        return if (previousSetWeight != null) {
            getDropSetWeight(lift.incrementOverride, previousSetWeight, set.dropPercentage)
        } else null
    }

    private fun getDropSetWeight(
        incrementOverride: Float?,
        previousSetWeight: Float,
        dropPercentage: Float,
    ): Float {
        val incrementAmount = incrementOverride
            ?: SettingsManager.getSetting(
                INCREMENT_AMOUNT,
                DEFAULT_INCREMENT_AMOUNT
            )

        return (previousSetWeight * (1 - dropPercentage)).roundToNearestFactor(
            incrementAmount
        )
    }

    protected fun List<LoggingStandardSet>.flattenWeightRecommendationsStandard(): List<LoggingStandardSet> {
        // Flattens out weight recommendations for all sets that use the same rep range and RPE target
        return if (this.distinctBy { it.weightRecommendation }.size > 1) {
            // Typically when there's a difference it's because you either added weight or dropped weight by the last set, so use
            // whatever the last weight recommendation is
            val lastSetWithRecommendation = this.filter { it.weightRecommendation != null }.maxByOrNull { it.position }
            this.fastMap { set ->
                set.copy(weightRecommendation = lastSetWithRecommendation?.weightRecommendation)
            }
        } else this
    }

    protected fun List<GenericLoggingSet>.flattenWeightRecommendationsGeneric(): List<GenericLoggingSet> {
        // Flattens out weight recommendations for all standard sets that use the same rep range and RPE target
        // Not really a good way I can think of to handle drop and myo rep sets, so let them be
        val standardSetRecommendations = this.filterIsInstance<LoggingStandardSet>()
            .flattenWeightRecommendationsStandard()
            .associate { it.position to it.weightRecommendation }

        return this.fastMap { set ->
            if (set is LoggingStandardSet) {
                set.copy(weightRecommendation = standardSetRecommendations[set.position] ?: set.weightRecommendation)
            } else set
        }
    }

    protected fun missedBottomRepRange(result: SetResult?, goals: CalculationStandardWorkoutLift): Boolean {
        return if (result != null) {
            missedBottomRepRange(
                repRangeBottom = goals.repRangeBottom,
                rpeTarget = goals.rpeTarget,
                completedReps = result.reps,
                completedRpe = result.rpe,
            )
        } else false
    }

    private fun missedBottomRepRange(result: SetResult?, repRangeBottom: Int, rpeTarget: Float): Boolean {
        return if (result != null) {
            missedBottomRepRange(
                repRangeBottom = repRangeBottom,
                rpeTarget = rpeTarget,
                completedReps = result.reps,
                completedRpe = result.rpe,
            )
        } else false
    }

    protected fun missedBottomRepRange(
        repRangeBottom: Int,
        rpeTarget: Float,
        completedReps: Int,
        completedRpe: Float,
    ): Boolean {
        val minRepsRequiredConsideringRpe = (repRangeBottom + (10 - rpeTarget)) - 1
        val repsConsideringRpe = completedReps + (10 - completedRpe)
        return repsConsideringRpe < minRepsRequiredConsideringRpe
    }

    protected fun incrementWeight(lift: CalculationWorkoutLift, prevSet: SetResult): Float {
        return prevSet.weight + (lift.incrementOverride
            ?: SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT)).toInt()
    }

    protected fun getCalculatedWeightRecommendation(
        increment: Float?,
        repGoal: Int,
        rpeTarget: Float,
        result: SetResult
    ): Float {
        val roundingFactor = (increment
            ?: SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT))

        return WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = result.weight,
            completedReps = result.reps,
            completedRpe = result.rpe,
            repGoal = repGoal,
            rpeGoal = rpeTarget,
            roundingFactor = roundingFactor)
    }

    protected fun getDropSetFailureWeight(
        incrementOverride: Float?,
        repRangeBottom: Int,
        rpeTarget: Float,
        dropPercentage: Float,
        result: SetResult?,
        droppedFromSetResult: SetResult?,
    ): Float? {
        return missedBottomRepRange(result, repRangeBottom, rpeTarget)
            .let { shouldDecrease ->
                if (shouldDecrease && result != null) {
                    getCalculatedWeightRecommendation(
                        increment = incrementOverride,
                        repGoal = repRangeBottom,
                        rpeTarget = rpeTarget,
                        result = result,
                    )
                } else result?.weight
                    ?: if (droppedFromSetResult?.weight != null) {
                        getDropSetWeight(
                            incrementOverride = incrementOverride,
                            previousSetWeight = droppedFromSetResult.weight,
                            dropPercentage = dropPercentage
                        )
                    } else null
            }
    }

    protected fun customSetMeetsCriterion(set: CalculationCustomLiftSet, previousSet: SetResult?): Boolean {
        return previousSet != null && set.rpeTarget >= previousSet.rpe && set.repRangeTop <= previousSet.reps
    }

    protected fun customSetShouldDecreaseWeight(set: CalculationCustomLiftSet, previousSet: SetResult?): Boolean {
        return if (previousSet != null) {
            missedBottomRepRange(
                repRangeBottom = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                completedReps = previousSet.reps,
                completedRpe = previousSet.rpe,
            )
        } else false
    }

    protected fun customSetMeetsCriterion(
        set: CalculationMyoRepSet,
        setData: List<MyoRepSetResult>?,
    ) : Boolean {
        if (setData.isNullOrEmpty()) return false

        val activationSet = setData.first()
        val myoRepSets = setData.filter { it.myoRepSetPosition != null }
        val criterionMet = set.repRangeTop <= activationSet.reps &&
                set.rpeTarget >= activationSet.rpe &&
                myoRepSets.all {
                    val rpeTarget = if (it.myoRepSetPosition == null) set.rpeTarget else 10f
                    rpeTarget >= it.rpe &&
                            it.weight == activationSet.weight
                }

        return criterionMet && if (set.setMatching) {
            set.setGoal >= myoRepSets.size && myoRepSets.sumOf { it.reps } >= set.repRangeTop
        } else {
            set.setGoal <= myoRepSets.size
        }
    }

    protected fun getPreviousSetResultLabel(result: SetResult?): String {
        return if (result != null) {
            "${result.weight.toString().removeSuffix(".0")}x${result.reps} @${result.rpe}"
        } else {
            "—"
        }
    }
}