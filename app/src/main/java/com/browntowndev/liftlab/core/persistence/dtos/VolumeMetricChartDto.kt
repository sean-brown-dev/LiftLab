package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

data class VolumeMetricChartDto(
    val id: Long = 0,
    val volumeType: VolumeType,
    val volumeTypeImpacts: List<VolumeTypeImpact>
) {
    val volumeTypeImpactsBitmask
        get() = volumeTypeImpacts.sumOf { it.bitmask }
}
