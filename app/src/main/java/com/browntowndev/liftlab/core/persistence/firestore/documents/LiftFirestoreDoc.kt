package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.MovementPattern

@Keep
data class LiftFirestoreDoc(
    override var id: Long = 0L,
    var name: String = "",
    var movementPattern: MovementPattern = MovementPattern.HORIZONTAL_PUSH,
    var volumeTypesBitmask: Int = 0,
    var secondaryVolumeTypesBitmask: Int? = null,
    var restTime: Long? = null,
    var restTimerEnabled: Boolean = false,
    var incrementOverride: Float? = null,
    var isHidden: Boolean = false,
    var isBodyweight: Boolean = false,
    var note: String? = null
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@LiftFirestoreDoc.firestoreId
            lastUpdated = this@LiftFirestoreDoc.lastUpdated
            synced = this@LiftFirestoreDoc.synced
        }
    }
}
