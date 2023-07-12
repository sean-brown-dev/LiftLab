package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

interface ProgressionFactory {
    fun calculate(
        workout: WorkoutDto,
        previousSetResults: List<SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
    ): LoggingWorkoutDto
}