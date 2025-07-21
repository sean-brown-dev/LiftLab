package com.browntowndev.liftlab.core.domain.models

import java.util.Date

data class Program(
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val deloadWeek: Int = 4,
    val currentMicrocycle: Int = 0,
    val currentMicrocyclePosition: Int = 0,
    val currentMesocycle: Int = 0,
    val workouts: List<Workout> = listOf(),
    val lastUpdated: Date? = null,
)