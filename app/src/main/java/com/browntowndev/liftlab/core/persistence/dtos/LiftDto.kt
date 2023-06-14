package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.displayName

data class LiftDto(
    val id: Long,
    val name: String,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val incrementOverride: Double?,
    val isHidden: Boolean,
    val isBodyweight: Boolean,
) {
    val movementPatternDisplayName get() = this.movementPattern.displayName()
}