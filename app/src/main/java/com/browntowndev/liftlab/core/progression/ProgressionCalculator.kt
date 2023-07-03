package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.ProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

interface ProgressionCalculator {
    fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
    ): List<ProgressionDto>
}