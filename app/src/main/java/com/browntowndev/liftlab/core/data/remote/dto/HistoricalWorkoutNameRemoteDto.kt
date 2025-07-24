package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep

@Keep
data class HistoricalWorkoutNameRemoteDto(
    var id: Long = 0L,
    var programId: Long = 0L,
    var workoutId: Long = 0L,
    var programName: String = "",
    var workoutName: String = ""
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@HistoricalWorkoutNameRemoteDto.remoteId
            lastUpdated = this@HistoricalWorkoutNameRemoteDto.lastUpdated
            deleted = this@HistoricalWorkoutNameRemoteDto.deleted
            synced = this@HistoricalWorkoutNameRemoteDto.synced
        }
    }
}
