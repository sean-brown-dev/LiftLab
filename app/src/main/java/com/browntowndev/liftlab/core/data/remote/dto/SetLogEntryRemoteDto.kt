package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class SetLogEntryRemoteDto(
    var id: Long = 0L,
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
    var setMatching: Boolean? = null,
    var maxSets: Int? = null,
    var repFloor: Int? = null,
    var dropPercentage: Float? = null,

    @get:PropertyName("isDeload")
    @set:PropertyName("isDeload")
    var isDeload: Boolean = false
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@SetLogEntryRemoteDto.remoteId
            lastUpdated = this@SetLogEntryRemoteDto.lastUpdated
            deleted = this@SetLogEntryRemoteDto.deleted
            synced = this@SetLogEntryRemoteDto.synced
        }
    }
}
