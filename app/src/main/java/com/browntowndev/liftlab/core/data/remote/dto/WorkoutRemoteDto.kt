package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep

@Keep
data class WorkoutRemoteDto(
    override var id: Long = 0L,
    var programId: Long = 0L,
    var name: String = "",
    var position: Int = 0
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@WorkoutRemoteDto.remoteId
            lastUpdated = this@WorkoutRemoteDto.lastUpdated
            synced = this@WorkoutRemoteDto.synced
        }
    }
}
