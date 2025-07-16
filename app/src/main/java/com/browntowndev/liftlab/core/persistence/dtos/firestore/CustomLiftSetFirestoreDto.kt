package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.SetType

@Keep
data class CustomLiftSetFirestoreDto(
    var id: Long = 0L,
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
): BaseFirestoreDto() {
    override fun copyWithBase(): BaseFirestoreDto {
        return this.copy().apply {
            firestoreId = this@CustomLiftSetFirestoreDto.firestoreId
            lastUpdated = this@CustomLiftSetFirestoreDto.lastUpdated
            synced = this@CustomLiftSetFirestoreDto.synced
        }
    }
}
