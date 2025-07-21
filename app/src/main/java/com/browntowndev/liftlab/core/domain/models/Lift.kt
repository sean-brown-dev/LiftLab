package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.displayName
import kotlin.time.Duration

data class Lift(
    val id: Long,
    val name: String,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val secondaryVolumeTypesBitmask: Int?,
    val incrementOverride: Float?,
    val restTime: Duration?,
    val restTimerEnabled: Boolean,
    val isHidden: Boolean,
    val isBodyweight: Boolean,
    val note: String?,
) {
    val movementPatternDisplayName get() = this.movementPattern.displayName()
}