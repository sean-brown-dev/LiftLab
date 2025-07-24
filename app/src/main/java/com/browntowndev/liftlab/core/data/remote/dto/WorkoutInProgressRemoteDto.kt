package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import java.util.Date

@Keep
data class WorkoutInProgressRemoteDto(
    var id: Long = 0L,
    var workoutId: Long = 0L,
    var startTime: Date = Date()
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@WorkoutInProgressRemoteDto.remoteId
            lastUpdated = this@WorkoutInProgressRemoteDto.lastUpdated
            deleted = this@WorkoutInProgressRemoteDto.deleted
            synced = this@WorkoutInProgressRemoteDto.synced
        }
    }
}
