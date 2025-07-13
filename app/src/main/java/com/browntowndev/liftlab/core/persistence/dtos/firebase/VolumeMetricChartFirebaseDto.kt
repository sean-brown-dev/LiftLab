package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

@Keep
data class VolumeMetricChartFirebaseDto(
    var id: Long = 0L,
    var volumeType: VolumeType = VolumeType.CHEST,
    var volumeTypeImpact: VolumeTypeImpact = VolumeTypeImpact.PRIMARY,
): BaseFirebaseDto()
