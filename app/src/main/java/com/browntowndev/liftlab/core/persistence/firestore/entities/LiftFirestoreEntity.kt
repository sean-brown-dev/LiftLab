package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.MovementPattern

@Keep
data class LiftFirestoreEntity(
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
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@LiftFirestoreEntity.firestoreId
            lastUpdated = this@LiftFirestoreEntity.lastUpdated
            synced = this@LiftFirestoreEntity.synced
        }
    }
}
