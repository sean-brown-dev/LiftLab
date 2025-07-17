package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class ProgramFirestoreDto(
    override var id: Long = 0L,
    var name: String = "",
    var deloadWeek: Int = 0,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false,
    var currentMicrocycle: Int = 0,
    var currentMicrocyclePosition: Int = 0,
    var currentMesocycle: Int = 0
): BaseFirestoreDto() {
    override fun copyWithBase(): BaseFirestoreDto {
        return this.copy().apply {
            firestoreId = this@ProgramFirestoreDto.firestoreId
            lastUpdated = this@ProgramFirestoreDto.lastUpdated
            synced = this@ProgramFirestoreDto.synced
        }
    }
}
