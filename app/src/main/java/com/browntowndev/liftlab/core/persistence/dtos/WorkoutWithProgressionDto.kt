package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

data class WorkoutWithProgressionDto(
    val workout: WorkoutDto,
    val progressions: Map<Long, List<ProgressionDto>>,
    val previousSetResults: Map<String, SetResult>,
)