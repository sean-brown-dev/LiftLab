package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutLogEntryFirestoreDto(
    var id: Long = 0L,
    var historicalWorkoutNameId: Long = 0L,
    var programWorkoutCount: Int = 0,
    var programDeloadWeek: Int = 0,
    var mesocycle: Int = 0,
    var microcycle: Int = 0,
    var microcyclePosition: Int = 0,
    var date: Date = Date(),
    var durationInMillis: Long = 0L
): BaseFirestoreDto() {
    override fun copyWithBase(): BaseFirestoreDto {
        return this.copy().apply {
            firestoreId = this@WorkoutLogEntryFirestoreDto.firestoreId
            lastUpdated = this@WorkoutLogEntryFirestoreDto.lastUpdated
            synced = this@WorkoutLogEntryFirestoreDto.synced
        }
    }
}
