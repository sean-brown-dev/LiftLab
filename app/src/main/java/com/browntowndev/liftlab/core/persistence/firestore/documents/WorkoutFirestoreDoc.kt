package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep

@Keep
data class WorkoutFirestoreDoc(
    override var id: Long = 0L,
    var programId: Long = 0L,
    var name: String = "",
    var position: Int = 0
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@WorkoutFirestoreDoc.firestoreId
            lastUpdated = this@WorkoutFirestoreDoc.lastUpdated
            synced = this@WorkoutFirestoreDoc.synced
        }
    }
}
