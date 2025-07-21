package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutInProgressFirestoreEntity(
    override var id: Long = 0L,
    var workoutId: Long = 0L,
    var startTime: Date = Date()
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@WorkoutInProgressFirestoreEntity.firestoreId
            lastUpdated = this@WorkoutInProgressFirestoreEntity.lastUpdated
            synced = this@WorkoutInProgressFirestoreEntity.synced
        }
    }
}
