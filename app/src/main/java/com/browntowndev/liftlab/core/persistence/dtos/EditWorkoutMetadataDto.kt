package com.browntowndev.liftlab.core.persistence.dtos

data class EditWorkoutMetadataDto(
    val workoutId: Long,
    val mesoCycle: Int,
    val microCycle: Int,
)
