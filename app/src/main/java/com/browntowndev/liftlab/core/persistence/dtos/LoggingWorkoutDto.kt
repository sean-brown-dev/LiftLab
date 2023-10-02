package com.browntowndev.liftlab.core.persistence.dtos

data class LoggingWorkoutDto(
    val id: Long,
    val name: String,
    val lifts: List<LoggingWorkoutLiftDto>,
)
