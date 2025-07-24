package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.MovementPattern

@Keep
data class LiftRemoteDto(
    var id: Long = 0L,
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
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@LiftRemoteDto.remoteId
            lastUpdated = this@LiftRemoteDto.lastUpdated
            deleted = this@LiftRemoteDto.deleted
            synced = this@LiftRemoteDto.synced
        }
    }
}
