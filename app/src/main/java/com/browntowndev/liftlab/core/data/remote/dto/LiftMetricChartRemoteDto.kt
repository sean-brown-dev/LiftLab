package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

@Keep
data class LiftMetricChartRemoteDto(
    var id: Long = 0L,
    var liftId: Long? = null,
    var chartType: LiftMetricChartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX,
): BaseRemoteDto() {
    override fun copyWithBase(): BaseRemoteDto {
        return this.copy().apply {
            remoteId = this@LiftMetricChartRemoteDto.remoteId
            lastUpdated = this@LiftMetricChartRemoteDto.lastUpdated
            deleted = this@LiftMetricChartRemoteDto.deleted
            synced = this@LiftMetricChartRemoteDto.synced
        }
    }
}
