package com.browntowndev.liftlab.core.domain.models.workoutLogging

data class RestTimerInProgressState(
    val totalRestTimeInMillis: Long? = null,
    val timeStartedInMillis: Long? = null,
)
