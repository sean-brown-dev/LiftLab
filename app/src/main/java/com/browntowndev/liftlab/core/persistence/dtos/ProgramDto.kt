package com.browntowndev.liftlab.core.persistence.dtos

data class ProgramDto(
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val deloadWeek: Int = 4,
    val currentMicrocycle: Int = 0,
    val currentMicrocyclePosition: Int = 0,
    val currentMesocycle: Int = 0,
    val workouts: List<WorkoutDto> = listOf()
)