package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutInProgressFirebaseDto(
    var id: Long = 0L,
    var workoutId: Long = 0L,
    var startTime: Date = Date()
): BaseFirebaseDto()
