package com.browntowndev.liftlab.core.domain.models.workoutCalculation

import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift

data class CalculationWorkout(
    val id: Long,
    val lifts: List<CalculationWorkoutLift>
)
