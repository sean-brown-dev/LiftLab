package com.browntowndev.liftlab.core.domain.useCase.progression

import androidx.compose.ui.util.fastAny
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift

class LinearProgressionCalculator: BaseWholeLiftProgressionCalculator() {
    override fun allSetsMetCriterion(
        lift: CalculationStandardWorkoutLift,
        previousSetResults: List<SetResult>
    ): Boolean {
        val hasInvalidSets = previousSetResults.fastAny { it !is LinearProgressionSetResult }
        if (hasInvalidSets) {
            throw Exception("Linear progression liftEntity contains invalid set type.")
        }

        return previousSetResults.isNotEmpty() &&
                previousSetResults.size == lift.setCount &&
                previousSetResults.all {
                    it.rpe <= lift.rpeTarget && it.reps >= lift.repRangeBottom
                }
    }

    override fun allSetsMetCriterion(
        lift: CalculationCustomWorkoutLift,
        previousSetResults: List<SetResult>
    ): Boolean {
        throw Exception("Linear progression lifts must be of type ${StandardWorkoutLift::class.simpleName}." +
                " LiftEntity provided was of type ${lift::class.simpleName}")
    }

    override fun getFailureWeight(
        workoutLift: CalculationWorkoutLift,
        previousSetResults: List<SetResult>,
        position: Int?,
    ): Float? {
        val previouslyFailedSet = previousSetResults.firstOrNull {
            (it as LinearProgressionSetResult).missedLpGoals > 1 // cast validated already by allSetsMetCriteria()
        }

        return if (previousSetResults.isNotEmpty() && previouslyFailedSet != null) {
            val factor =
                workoutLift.incrementOverride ?: SettingsManager.getSetting(
                    SettingsManager.SettingNames.INCREMENT_AMOUNT,
                    SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
                )
            (previouslyFailedSet.weight * .9).roundToNearestFactor(factor)
        } else super.getFailureWeight(workoutLift = workoutLift, previousSetResults = previousSetResults, position = null)
    }
}