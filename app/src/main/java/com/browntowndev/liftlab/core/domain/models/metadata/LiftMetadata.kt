package com.browntowndev.liftlab.core.domain.models.metadata

import com.browntowndev.liftlab.core.common.enums.MovementPattern

data class LiftMetadata(
    val id: Long,
    val name: String,
    val note: String?,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val secondaryVolumeTypesBitmask: Int?
)
