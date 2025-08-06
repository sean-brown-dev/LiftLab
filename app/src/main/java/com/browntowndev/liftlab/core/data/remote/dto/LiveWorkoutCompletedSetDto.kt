package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.domain.enums.SetType

@Keep
data class LiveWorkoutCompletedSetDto(
    var id: Long = 0L,
    var workoutId: Long = 0L,
    var liftId: Long = 0L,
    var setType: SetType = SetType.STANDARD,
    var liftPosition: Int = 0,
    var setPosition: Int = 0,
    var myoRepSetPosition: Int? = null,
    var weight: Float = 0f,
    var reps: Int = 0,
    var rpe: Float = 0f,
    var oneRepMax: Int = 0,
    var missedLpGoals: Int? = null,
    var isDeload: Boolean = false
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@LiveWorkoutCompletedSetDto.remoteId
            lastUpdated = this@LiveWorkoutCompletedSetDto.lastUpdated
            deleted = this@LiveWorkoutCompletedSetDto.deleted
            synced = this@LiveWorkoutCompletedSetDto.synced
        }
    }
}
