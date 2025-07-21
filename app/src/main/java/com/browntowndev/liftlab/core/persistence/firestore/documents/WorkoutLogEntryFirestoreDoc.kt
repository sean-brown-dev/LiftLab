package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutLogEntryFirestoreDoc(
    override var id: Long = 0L,
    var historicalWorkoutNameId: Long = 0L,
    var programWorkoutCount: Int = 0,
    var programDeloadWeek: Int = 0,
    var mesocycle: Int = 0,
    var microcycle: Int = 0,
    var microcyclePosition: Int = 0,
    var date: Date = Date(),
    var durationInMillis: Long = 0L
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@WorkoutLogEntryFirestoreDoc.firestoreId
            lastUpdated = this@WorkoutLogEntryFirestoreDoc.lastUpdated
            synced = this@WorkoutLogEntryFirestoreDoc.synced
        }
    }
}
