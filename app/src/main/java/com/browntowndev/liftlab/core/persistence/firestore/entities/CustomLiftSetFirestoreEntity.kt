package com.browntowndev.liftlab.core.persistence.firestore.entities

import android.util.Log
import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.SetType

@Keep
data class CustomLiftSetFirestoreEntity(
    override var id: Long = 0L,
    var workoutLiftId: Long = 0L,
    var type: SetType = SetType.STANDARD,
    var position: Int = 0,
    var rpeTarget: Float = 0f,
    var repRangeBottom: Int = 0,
    var repRangeTop: Int = 0,
    var setGoal: Int? = null,
    var repFloor: Int? = null,
    var dropPercentage: Float? = null,
    var maxSets: Int? = null,
    var setMatching: Boolean = false
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        Log.d("CustomLiftSetFirestoreEntity", "copyWithBase: $this")
        return this.copy().apply {
            firestoreId = this@CustomLiftSetFirestoreEntity.firestoreId
            lastUpdated = this@CustomLiftSetFirestoreEntity.lastUpdated
            synced = this@CustomLiftSetFirestoreEntity.synced
        }
    }
}
