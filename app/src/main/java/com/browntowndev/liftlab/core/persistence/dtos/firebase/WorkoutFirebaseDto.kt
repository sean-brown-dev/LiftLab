package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep

@Keep
data class WorkoutFirebaseDto(
    var id: Long = 0L,
    var programId: Long = 0L,
    var name: String = "",
    var position: Int = 0
): BaseFirebaseDto()
