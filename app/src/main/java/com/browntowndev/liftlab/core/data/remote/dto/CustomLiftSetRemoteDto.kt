package com.browntowndev.liftlab.core.data.remote.dto

import android.util.Log
import androidx.annotation.Keep
import com.browntowndev.liftlab.core.domain.enums.SetType

@Keep
data class CustomLiftSetRemoteDto(
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
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        Log.d("CustomLiftSetRemoteDto", "copyWithBase: $this")
        return this.copy().apply {
            remoteId = this@CustomLiftSetRemoteDto.remoteId
            lastUpdated = this@CustomLiftSetRemoteDto.lastUpdated
            deleted = this@CustomLiftSetRemoteDto.deleted
            synced = this@CustomLiftSetRemoteDto.synced
        }
    }
}
