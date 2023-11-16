package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

abstract class BaseProgressionCalculator: ProgressionCalculator {
    protected fun getDropSetRecommendation(
        lift: GenericWorkoutLift,
        set: DropSetDto,
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
                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                5f
            )

        return (previousSetWeight * (1 - dropPercentage)).roundToNearestFactor(
            incrementAmount
        )
    }

    protected fun List<LoggingStandardSetDto>.flattenWeightRecommendationsStandard(): List<LoggingStandardSetDto> {
        // Flattens out weight recommendations for all sets that use the same rep range and RPE target
        return if (this.distinctBy { it.weightRecommendation }.size > 1) {
            this.groupBy {
                "${it.repRangeBottom}-${it.repRangeTop}-${it.rpeTarget}"
            }.flatMap {
                val minWeight = it.value.minOf { set -> set.weightRecommendation ?: Float.MAX_VALUE }
                it.value.fastMap { set ->
                    set.copy(weightRecommendation = minWeight)
                }
            }
        } else this
    }

    protected fun List<GenericLoggingSet>.flattenWeightRecommendationsGeneric(): List<GenericLoggingSet> {
        // Flattens out weight recommendations for all standard sets that use the same rep range and RPE target
        // Not really a good way I can think of to handle drop and myo rep sets, so let them be
        val standardSets = this.filterIsInstance<LoggingStandardSetDto>()
        return standardSets.flattenWeightRecommendationsStandard()
    }

    protected fun shouldDecreaseWeight(result: SetResult?, goals: StandardWorkoutLiftDto): Boolean {
        return if (result != null) {
            val minimumRepsAllowed = goals.repRangeBottom - 1
            val repsConsideringRpe = result.reps + (10 - result.rpe)

            repsConsideringRpe < minimumRepsAllowed
        } else false
    }

    private fun shouldDecreaseWeight(result: SetResult?, repRangeBottom: Int): Boolean {
        return if (result != null) {
            val minimumRepsAllowed = repRangeBottom - 1
            val repsConsideringRpe = result.reps + (10 - result.rpe)

            repsConsideringRpe < minimumRepsAllowed
        } else false
    }

    protected fun incrementWeight(lift: GenericWorkoutLift, prevSet: SetResult): Float {
        return prevSet.weight + (lift.incrementOverride
            ?: SettingsManager.getSetting(SettingsManager.SettingNames.INCREMENT_AMOUNT, 5f)).toInt()
    }

    protected fun decreaseWeight(
        incrementOverride: Float?,
        repRangeBottom: Int,
        rpeTarget: Float,
        prevSet: SetResult
    ): Float {
        val roundingFactor = (incrementOverride
            ?: SettingsManager.getSetting(SettingsManager.SettingNames.INCREMENT_AMOUNT, 5f))

        return CalculationEngine.calculateSuggestedWeight(
            completedWeight = prevSet.weight,
            completedReps = prevSet.reps,
            completedRpe = prevSet.rpe,
            repGoal = repRangeBottom,
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
        return shouldDecreaseWeight(result, repRangeBottom).let { shouldDecrease ->
            if (shouldDecrease && result != null) {
                decreaseWeight(
                    incrementOverride = incrementOverride,
                    repRangeBottom = repRangeBottom,
                    rpeTarget = rpeTarget,
                    prevSet = result,
                )
            } else result?.weight
                ?: if (droppedFromSetResult?.weight != null) {
                    getDropSetWeight(
                        incrementOverride = incrementOverride,
                        previousSetWeight = droppedFromSetResult.weight,
                        dropPercentage = dropPercentage)
                } else null
        }
    }

    protected fun customSetMeetsCriterion(set: GenericLiftSet, previousSet: SetResult?): Boolean {
        return previousSet != null && set.rpeTarget >= previousSet.rpe && set.repRangeTop <= previousSet.reps
    }

    protected fun customSetShouldDecreaseWeight(set: GenericLiftSet, previousSet: SetResult?): Boolean {
        return if (previousSet != null) {
            val minRepsRequiredConsideringRpe = (set.repRangeBottom + (10 - set.rpeTarget)) - 1
            val repsConsideringRpe = previousSet.reps + (10 - previousSet.rpe)
            repsConsideringRpe < minRepsRequiredConsideringRpe
        } else false
    }

    protected fun customSetMeetsCriterion(
        set: MyoRepSetDto,
        setData: List<MyoRepSetResultDto>?,
    ) : Boolean {
        if (setData == null) return false

        val activationSet = setData.first()
        val myoRepSets = setData.filter { it.myoRepSetPosition != null }
        val criterionMet = set.repRangeTop <= activationSet.reps &&
                set.rpeTarget >= activationSet.rpe &&
                (myoRepSets.all { set.rpeTarget >= it.rpe && it.weight == activationSet.weight })

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