package com.browntowndev.liftlab.core.domain.models

data class HistoricalWorkoutName(
    val id: Long = 0,
    val programId: Long,
    val workoutId: Long,
    val programName: String,
    val workoutName: String,
)
