package com.browntowndev.liftlab.core.domain.useCase.progression

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult

class DoubleProgressionCalculator: BaseWholeLiftProgressionCalculator() {
    override fun allSetsMetCriterion(
        lift: CalculationStandardWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean {
        val firstSet = previousSetResults.firstOrNull()
        val allSetGoalsMet = previousSetResults.all {
            it.reps >= lift.repRangeTop && it.rpe <= lift.rpeTarget
        }

        return allSetGoalsMet
    }

    override fun allSetsMetCriterion(
        lift: CalculationCustomWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean {
        val groupedSetData = previousSetResults.groupBy { it.setPosition }
        val allSetsMetCriterion =
            groupedSetData.size == lift.setCount &&
                    lift.customLiftSets.all { set ->
                        when (set) {
                            is CalculationMyoRepSet -> {
                                customSetMeetsCriterion(
                                    set = set,
                                    setData = groupedSetData[set.position]?.filterIsInstance<MyoRepSetResult>()
                                )
                            }
                            else -> {
                                customSetMeetsCriterion(
                                    set = set,
                                    previousSet = groupedSetData[set.position]?.firstOrNull()
                                )
                            }
                        }
                    }

        return allSetsMetCriterion
    }
}