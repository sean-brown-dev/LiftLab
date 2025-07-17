package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep

@Keep
data class HistoricalWorkoutNameFirestoreDto(
    override var id: Long = 0L,
    var programId: Long = 0L,
    var workoutId: Long = 0L,
    var programName: String = "",
    var workoutName: String = ""
): BaseFirestoreDto() {
    override fun copyWithBase(): BaseFirestoreDto {
        return this.copy().apply {
            firestoreId = this@HistoricalWorkoutNameFirestoreDto.firestoreId
            lastUpdated = this@HistoricalWorkoutNameFirestoreDto.lastUpdated
            synced = this@HistoricalWorkoutNameFirestoreDto.synced
        }
    }
}
