package com.browntowndev.liftlab.core.domain.models

data class ActiveProgramMetadata(
    val programId: Long,
    val name: String,
    val deloadWeek: Int,
    val currentMesocycle: Int,
    val currentMicrocycle: Int,
    val currentMicrocyclePosition: Int,
    val workoutCount: Int,
)