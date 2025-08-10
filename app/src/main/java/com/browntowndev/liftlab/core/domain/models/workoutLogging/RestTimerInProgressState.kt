package com.browntowndev.liftlab.core.domain.models.workoutLogging

data class RestTimerInProgressState(
    val totalRestTime: Long? = null,
    val timeStartedInMillis: Long? = null,
)
