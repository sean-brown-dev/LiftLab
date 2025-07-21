package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutInProgressFirestoreDoc(
    override var id: Long = 0L,
    var workoutId: Long = 0L,
    var startTime: Date = Date()
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@WorkoutInProgressFirestoreDoc.firestoreId
            lastUpdated = this@WorkoutInProgressFirestoreDoc.lastUpdated
            synced = this@WorkoutInProgressFirestoreDoc.synced
        }
    }
}
