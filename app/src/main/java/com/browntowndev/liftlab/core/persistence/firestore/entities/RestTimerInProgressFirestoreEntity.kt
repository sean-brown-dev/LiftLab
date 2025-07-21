package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep

@Keep
data class RestTimerInProgressFirestoreEntity(
    override var id: Long = 0L,
    var timeStartedInMillis: Long = 0L,
    var restTime: Long = 0L
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@RestTimerInProgressFirestoreEntity.firestoreId
            lastUpdated = this@RestTimerInProgressFirestoreEntity.lastUpdated
            synced = this@RestTimerInProgressFirestoreEntity.synced
        }
    }
}
