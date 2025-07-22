package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

@Keep
data class VolumeMetricChartRemoteDto(
    override var id: Long = 0L,
    var volumeType: VolumeType = VolumeType.CHEST,
    var volumeTypeImpact: VolumeTypeImpact = VolumeTypeImpact.PRIMARY,
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@VolumeMetricChartRemoteDto.remoteId
            lastUpdated = this@VolumeMetricChartRemoteDto.lastUpdated
            synced = this@VolumeMetricChartRemoteDto.synced
        }
    }
}
