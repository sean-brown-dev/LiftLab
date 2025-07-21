package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep

@Keep
data class HistoricalWorkoutNameFirestoreEntity(
    override var id: Long = 0L,
    var programId: Long = 0L,
    var workoutId: Long = 0L,
    var programName: String = "",
    var workoutName: String = ""
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@HistoricalWorkoutNameFirestoreEntity.firestoreId
            lastUpdated = this@HistoricalWorkoutNameFirestoreEntity.lastUpdated
            synced = this@HistoricalWorkoutNameFirestoreEntity.synced
        }
    }
}
