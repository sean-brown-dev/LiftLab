package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastAny
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class LinearProgressionCalculator: StraightSetProgressionCalculator() {
    override fun allSetsMetCriterion(
        lift: StandardWorkoutLiftDto,
        previousSetResults: List<SetResult>
    ): Boolean {
        val hasInvalidSets = previousSetResults.fastAny { it !is LinearProgressionSetResultDto }
        if (hasInvalidSets) {
            throw Exception("Linear progression lift contains invalid set type.")
        }

        return previousSetResults.size == lift.setCount &&
                previousSetResults.isNotEmpty() &&
                previousSetResults.all {
                    it.rpe == lift.rpeTarget && it.reps == lift.repRangeTop
                }
    }

    override fun allSetsMetCriterion(
        lift: CustomWorkoutLiftDto,
        previousSetResults: List<SetResult>
    ): Boolean {
        throw Exception("Linear progression lifts must be of type ${StandardWorkoutLiftDto::class.simpleName}." +
                " Lift provided was of type ${lift::class.simpleName}")
    }

    override fun getFailureWeight(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>
    ): Float? {
        val previouslyFailedSet = previousSetResults.firstOrNull {
            (it as LinearProgressionSetResultDto).missedLpGoals > 1 // cast validated already by allSetsMetCriteria()
        }

        return if (previousSetResults.isNotEmpty() && previouslyFailedSet != null) {
            val factor =
                workoutLift.incrementOverride ?: workoutLift.liftIncrementOverride ?: SettingsManager.getSetting(
                    SettingsManager.SettingNames.INCREMENT_AMOUNT,
                    5f
                )
            (previouslyFailedSet.weight * .9).roundToNearestFactor(factor)
        } else super.getFailureWeight(workoutLift = workoutLift, previousSetResults = previousSetResults)
    }
}