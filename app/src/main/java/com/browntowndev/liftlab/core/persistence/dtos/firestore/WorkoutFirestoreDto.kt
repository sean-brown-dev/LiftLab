package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep

@Keep
data class WorkoutFirestoreDto(
    var id: Long = 0L,
    var programId: Long = 0L,
    var name: String = "",
    var position: Int = 0
): BaseFirestoreDto()
