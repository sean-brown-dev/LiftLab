package com.browntowndev.liftlab.ui.models.workout

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.displayName
import kotlin.time.Duration

data class LiftUiModel(
    val id: Long,
    val name: String,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val secondaryVolumeTypesBitmask: Int?,
    val incrementOverride: Float?,
    val restTime: Duration?,
    val restTimerEnabled: Boolean,
    val isBodyweight: Boolean,
    val note: String?,
) {
    val movementPatternDisplayName get() = this.movementPattern.displayName()
}