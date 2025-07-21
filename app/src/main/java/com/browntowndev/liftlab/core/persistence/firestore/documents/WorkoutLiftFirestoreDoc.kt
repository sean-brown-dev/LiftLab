package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme

@Keep
data class WorkoutLiftFirestoreDoc(
    override var id: Long = 0L,
    var workoutId: Long = 0L,
    var liftId: Long = 0L,
    var progressionScheme: ProgressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
    var position: Int = 0,
    var setCount: Int = 0,
    var deloadWeek: Int? = null,
    var rpeTarget: Float? = null,
    var repRangeBottom: Int? = null,
    var repRangeTop: Int? = null,
    var stepSize: Int? = null
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@WorkoutLiftFirestoreDoc.firestoreId
            lastUpdated = this@WorkoutLiftFirestoreDoc.lastUpdated
            synced = this@WorkoutLiftFirestoreDoc.synced
        }
    }
}
