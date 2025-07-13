package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import kotlin.time.Duration

@Keep
data class LiftFirebaseDto(
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
): BaseFirebaseDto()
