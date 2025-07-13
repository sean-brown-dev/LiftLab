package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutLogEntryFirebaseDto(
    var id: Long = 0L,
    var historicalWorkoutNameId: Long = 0L,
    var programWorkoutCount: Int = 0,
    var programDeloadWeek: Int = 0,
    var mesocycle: Int = 0,
    var microcycle: Int = 0,
    var microcyclePosition: Int = 0,
    var date: Date = Date(),
    var durationInMillis: Long = 0L
): BaseFirebaseDto()
