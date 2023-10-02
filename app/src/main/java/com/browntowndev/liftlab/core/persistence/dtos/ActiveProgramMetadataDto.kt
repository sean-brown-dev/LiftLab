package com.browntowndev.liftlab.core.persistence.dtos

data class ActiveProgramMetadataDto(
    val programId: Long,
    val name: String,
    val deloadWeek: Int,
    val currentMesocycle: Int,
    val currentMicrocycle: Int,
    val currentMicrocyclePosition: Int,
    val workoutCount: Int,
)