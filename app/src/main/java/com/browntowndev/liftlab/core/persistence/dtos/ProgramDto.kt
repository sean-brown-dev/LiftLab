package com.browntowndev.liftlab.core.persistence.dtos

data class ProgramDto(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val deloadWeek: Int,
    val currentMicrocycle: Int,
    val currentMicrocyclePosition: Int,
    val currentMesocycle: Int,
    val workouts: List<WorkoutDto>
)