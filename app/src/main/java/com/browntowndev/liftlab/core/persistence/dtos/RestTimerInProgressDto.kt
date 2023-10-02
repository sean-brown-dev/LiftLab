package com.browntowndev.liftlab.core.persistence.dtos

data class RestTimerInProgressDto(
    val id: Long = 0,
    val timeStartedInMillis: Long,
    val restTime: Long,
)
