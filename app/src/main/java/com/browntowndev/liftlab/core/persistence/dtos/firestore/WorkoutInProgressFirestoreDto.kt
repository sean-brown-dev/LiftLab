package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutInProgressFirestoreDto(
    var id: Long = 0L,
    var workoutId: Long = 0L,
    var startTime: Date = Date()
): BaseFirestoreDto()
