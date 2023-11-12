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
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

abstract class BaseProgressionCalculator: ProgressionCalculator {
    protected fun getDropSetRecommendation(
        lift: GenericWorkoutLift,
        set: DropSetDto,
        previousSetWeight: Float?,
    ): Float? {
        return if (previousSetWeight != null) {
            val incrementAmount = lift.incrementOverride
                ?: SettingsManager.getSetting(
                    SettingsManager.SettingNames.INCREMENT_AMOUNT,
                    5f
                )

            (previousSetWeight * (1 - set.dropPercentage)).roundToNearestFactor(
                incrementAmount
            )
        } else null
    }

    protected fun List<LoggingStandardSetDto>.flattenWeightRecommendations(): List<LoggingStandardSetDto> {
        return if (this.distinctBy { it.weightRecommendation }.size > 1) {
            val minWeight = this.minOf { it.weightRecommendation ?: Float.MAX_VALUE }
            this.fastMap { it.copy(weightRecommendation = minWeight) }
        } else this
    }

    protected fun shouldDecreaseWeight(result: SetResult?, goals: StandardWorkoutLiftDto): Boolean {
        return if (result != null) {
            val minimumRepsAllowed = goals.repRangeBottom - 1
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

    protected fun customSetMeetsCriterion(set: GenericLiftSet, previousSet: SetResult?): Boolean {
        return previousSet != null && set.rpeTarget >= previousSet.rpe && set.repRangeTop <= previousSet.reps
    }

    protected fun customSetShouldDecreaseWeight(set: GenericLiftSet, previousSet: SetResult?): Boolean {
        return if (previousSet != null) {
            val minRepsRequired = set.repRangeBottom - 1
            val repsConsideringRpe = previousSet.reps + (10 - previousSet.rpe)
            repsConsideringRpe < minRepsRequired
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