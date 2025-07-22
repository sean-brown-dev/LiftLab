package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep

@Keep
data class RestTimerInProgressRemoteDto(
    var id: Long = 0L,
    var timeStartedInMillis: Long = 0L,
    var restTime: Long = 0L
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@RestTimerInProgressRemoteDto.remoteId
            lastUpdated = this@RestTimerInProgressRemoteDto.lastUpdated
            synced = this@RestTimerInProgressRemoteDto.synced
        }
    }
}
