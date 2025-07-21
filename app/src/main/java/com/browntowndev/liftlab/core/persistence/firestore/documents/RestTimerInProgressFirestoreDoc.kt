package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep

@Keep
data class RestTimerInProgressFirestoreDoc(
    override var id: Long = 0L,
    var timeStartedInMillis: Long = 0L,
    var restTime: Long = 0L
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@RestTimerInProgressFirestoreDoc.firestoreId
            lastUpdated = this@RestTimerInProgressFirestoreDoc.lastUpdated
            synced = this@RestTimerInProgressFirestoreDoc.synced
        }
    }
}
