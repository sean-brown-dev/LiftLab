package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class ProgramFirestoreEntity(
    override var id: Long = 0L,
    var name: String = "",
    var deloadWeek: Int = 0,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false,
    var currentMicrocycle: Int = 0,
    var currentMicrocyclePosition: Int = 0,
    var currentMesocycle: Int = 0
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@ProgramFirestoreEntity.firestoreId
            lastUpdated = this@ProgramFirestoreEntity.lastUpdated
            synced = this@ProgramFirestoreEntity.synced
        }
    }
}
