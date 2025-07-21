package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep

@Keep
data class HistoricalWorkoutNameFirestoreDoc(
    override var id: Long = 0L,
    var programId: Long = 0L,
    var workoutId: Long = 0L,
    var programName: String = "",
    var workoutName: String = ""
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@HistoricalWorkoutNameFirestoreDoc.firestoreId
            lastUpdated = this@HistoricalWorkoutNameFirestoreDoc.lastUpdated
            synced = this@HistoricalWorkoutNameFirestoreDoc.synced
        }
    }
}
