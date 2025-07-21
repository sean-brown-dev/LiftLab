package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme

@Keep
data class WorkoutLiftFirestoreEntity(
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
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@WorkoutLiftFirestoreEntity.firestoreId
            lastUpdated = this@WorkoutLiftFirestoreEntity.lastUpdated
            synced = this@WorkoutLiftFirestoreEntity.synced
        }
    }
}
