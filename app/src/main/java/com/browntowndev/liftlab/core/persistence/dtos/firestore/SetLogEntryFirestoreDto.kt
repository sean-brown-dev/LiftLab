package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.google.firebase.firestore.PropertyName

@Keep
data class SetLogEntryFirestoreDto(
    override var id: Long = 0L,
    var workoutLogEntryId: Long = 0L,
    var liftId: Long = 0L,
    var workoutLiftDeloadWeek: Int? = null,
    var liftName: String = "",
    var liftMovementPattern: MovementPattern = MovementPattern.HORIZONTAL_PUSH,
    var progressionScheme: ProgressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
    var setType: SetType = SetType.STANDARD,
    var liftPosition: Int = 0,
    var setPosition: Int = 0,
    var myoRepSetPosition: Int? = null,
    var repRangeTop: Int? = null,
    var repRangeBottom: Int? = null,
    var rpeTarget: Float = 0f,
    var weightRecommendation: Float? = null,
    var weight: Float = 0f,
    var reps: Int = 0,
    var rpe: Float = 0f,
    var oneRepMax: Int = 0,
    var mesoCycle: Int = 0,
    var microCycle: Int = 0,
    var setMatching: Boolean? = null,
    var maxSets: Int? = null,
    var repFloor: Int? = null,
    var dropPercentage: Float? = null,

    @get:PropertyName("isDeload")
    @set:PropertyName("isDeload")
    var isDeload: Boolean = false
): BaseFirestoreDto() {
    override fun copyWithBase(): BaseFirestoreDto {
        return this.copy().apply {
            firestoreId = this@SetLogEntryFirestoreDto.firestoreId
            lastUpdated = this@SetLogEntryFirestoreDto.lastUpdated
            synced = this@SetLogEntryFirestoreDto.synced
        }
    }
}
