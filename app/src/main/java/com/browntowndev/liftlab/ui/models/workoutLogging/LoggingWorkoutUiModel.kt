package com.browntowndev.liftlab.ui.models.workoutLogging

data class LoggingWorkoutUiModel(
    val id: Long,
    val name: String = "",
    val lifts: List<LoggingWorkoutLiftUiModel>,
)
