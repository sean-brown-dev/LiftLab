package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection

@Keep
data class VolumeMetricChartRemoteDto(
    var id: Long = 0L,
    var volumeType: VolumeType = VolumeType.CHEST,
    var volumeTypeImpactSelection: VolumeTypeImpactSelection = VolumeTypeImpactSelection.PRIMARY,
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@VolumeMetricChartRemoteDto.remoteId
            lastUpdated = this@VolumeMetricChartRemoteDto.lastUpdated
            deleted = this@VolumeMetricChartRemoteDto.deleted
            synced = this@VolumeMetricChartRemoteDto.synced
        }
    }
}
