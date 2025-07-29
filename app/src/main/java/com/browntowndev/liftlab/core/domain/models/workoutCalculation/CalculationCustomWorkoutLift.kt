package com.browntowndev.liftlab.core.domain.models.workoutCalculation

import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift

data class CalculationCustomWorkoutLift(
    override val id: Long,
    override val liftId: Long,
    override val position: Int,
    override val progressionScheme: ProgressionScheme,
    override val deloadWeek: Int?,
    override val incrementOverride: Float?,
    val customLiftSets: List<CalculationCustomLiftSet>,
): CalculationWorkoutLift {
    override val setCount: Int
        get() = customLiftSets.size
}
