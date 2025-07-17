package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutInProgressFirestoreDto(
    override var id: Long = 0L,
    var workoutId: Long = 0L,
    var startTime: Date = Date()
): BaseFirestoreDto() {
    override fun copyWithBase(): BaseFirestoreDto {
        return this.copy().apply {
            firestoreId = this@WorkoutInProgressFirestoreDto.firestoreId
            lastUpdated = this@WorkoutInProgressFirestoreDto.lastUpdated
            synced = this@WorkoutInProgressFirestoreDto.synced
        }
    }
}
