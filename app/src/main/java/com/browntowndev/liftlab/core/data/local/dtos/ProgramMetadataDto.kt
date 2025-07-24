package com.browntowndev.liftlab.core.data.local.dtos

data class ProgramMetadataDto(
    val programId: Long,
    val name: String,
    val deloadWeek: Int,
    val currentMesocycle: Int,
    val currentMicrocycle: Int,
    val currentMicrocyclePosition: Int,
    val workoutCount: Int,
)
