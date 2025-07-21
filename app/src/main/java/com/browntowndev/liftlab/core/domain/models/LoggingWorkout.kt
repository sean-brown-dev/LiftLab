package com.browntowndev.liftlab.core.domain.models

data class LoggingWorkout(
    val id: Long,
    val name: String,
    val lifts: List<LoggingWorkoutLift>,
)
