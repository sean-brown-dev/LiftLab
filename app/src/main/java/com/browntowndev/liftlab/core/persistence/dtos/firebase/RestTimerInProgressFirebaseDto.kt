package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep

@Keep
data class RestTimerInProgressFirebaseDto(
    var id: Long = 0L,
    var timeStartedInMillis: Long = 0L,
    var restTime: Long = 0L
): BaseFirebaseDto()
