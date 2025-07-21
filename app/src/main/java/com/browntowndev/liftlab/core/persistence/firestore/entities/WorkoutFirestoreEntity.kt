package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep

@Keep
data class WorkoutFirestoreEntity(
    override var id: Long = 0L,
    var programId: Long = 0L,
    var name: String = "",
    var position: Int = 0
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@WorkoutFirestoreEntity.firestoreId
            lastUpdated = this@WorkoutFirestoreEntity.lastUpdated
            synced = this@WorkoutFirestoreEntity.synced
        }
    }
}
