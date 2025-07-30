package com.browntowndev.liftlab.core.domain.models.workoutLogging

data class RestTimerInProgress(
    val id: Long = 0,
    val timeStartedInMillis: Long,
    val restTime: Long,
)