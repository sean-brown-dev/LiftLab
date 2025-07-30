package com.browntowndev.liftlab.core.domain.models.metrics

import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

data class VolumeMetricChart(
    val id: Long = 0,
    val volumeType: VolumeType,
    val volumeTypeImpact: VolumeTypeImpact
)
