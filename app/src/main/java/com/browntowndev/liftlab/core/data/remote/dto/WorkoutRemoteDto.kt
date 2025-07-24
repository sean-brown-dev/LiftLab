package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep

@Keep
data class WorkoutRemoteDto(
    var id: Long = 0L,
    var programId: Long = 0L,
    var name: String = "",
    var position: Int = 0
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@WorkoutRemoteDto.remoteId
            lastUpdated = this@WorkoutRemoteDto.lastUpdated
            deleted = this@WorkoutRemoteDto.deleted
            synced = this@WorkoutRemoteDto.synced
        }
    }
}
