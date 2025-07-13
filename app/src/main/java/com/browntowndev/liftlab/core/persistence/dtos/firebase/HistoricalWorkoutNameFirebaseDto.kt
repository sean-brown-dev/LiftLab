package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep

@Keep
data class HistoricalWorkoutNameFirebaseDto(
    var id: Long = 0L,
    var programId: Long = 0L,
    var workoutId: Long = 0L,
    var programName: String = "",
    var workoutName: String = ""
): BaseFirebaseDto()
