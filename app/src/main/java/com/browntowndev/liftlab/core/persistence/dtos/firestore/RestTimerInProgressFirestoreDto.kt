package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep

@Keep
data class RestTimerInProgressFirestoreDto(
    var id: Long = 0L,
    var timeStartedInMillis: Long = 0L,
    var restTime: Long = 0L
): BaseFirestoreDto()
