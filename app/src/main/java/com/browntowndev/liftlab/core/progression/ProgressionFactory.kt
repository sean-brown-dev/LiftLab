package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutWithProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

interface ProgressionFactory {
    fun calculate(
        programDeloadWeek: Int,
        workout: WorkoutDto,
        previousSetResults: List<SetResult>,
    ): WorkoutWithProgressionDto
}