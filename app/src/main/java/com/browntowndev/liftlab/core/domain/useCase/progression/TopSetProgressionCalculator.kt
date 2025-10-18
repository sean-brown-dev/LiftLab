package com.browntowndev.liftlab.core.domain.useCase.progression

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift

class TopSetProgressionCalculator : BaseWholeLiftProgressionCalculator() {
    override fun allSetsMetCriterion(
        lift: CalculationStandardWorkoutLift,
        previousSetResults: List<SetResult>
    ): Boolean {
        if (previousSetResults.isEmpty()) return false
        val firstSetResult = previousSetResults.minBy { it.setPosition }
        return firstSetResult.reps >= lift.repRangeTop
    }

    override fun allSetsMetCriterion(
        lift: CalculationCustomWorkoutLift,
        previousSetResults: List<SetResult>
    ): Boolean {
        if (previousSetResults.isEmpty()) return false
        val firstSetResult = previousSetResults.minBy { it.setPosition }
        val firstSet = lift.customLiftSets.minBy { it.position }
        return firstSetResult.reps >= firstSet.repRangeTop
    }
}