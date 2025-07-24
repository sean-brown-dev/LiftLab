package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class ProgramRemoteDto(
    var id: Long = 0L,
    var name: String = "",
    var deloadWeek: Int = 0,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false,
    var currentMicrocycle: Int = 0,
    var currentMicrocyclePosition: Int = 0,
    var currentMesocycle: Int = 0
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@ProgramRemoteDto.remoteId
            lastUpdated = this@ProgramRemoteDto.lastUpdated
            deleted = this@ProgramRemoteDto.deleted
            synced = this@ProgramRemoteDto.synced
        }
    }
}
