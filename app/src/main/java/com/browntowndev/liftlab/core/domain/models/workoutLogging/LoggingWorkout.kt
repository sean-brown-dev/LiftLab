package com.browntowndev.liftlab.core.domain.models.workoutLogging

data class LoggingWorkout(
    val id: Long,
    val name: String = "",
    val lifts: List<LoggingWorkoutLift>,
)
