package com.browntowndev.liftlab.core.persistence.dtos

data class WorkoutWithProgressionDto(
    val workout: WorkoutDto,
    val progressions: Map<Long, List<ProgressionDto>>
)