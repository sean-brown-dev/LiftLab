package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

@Keep
data class LiftMetricChartFirestoreDto(
    override var id: Long = 0L,
    var liftId: Long? = null,
    var chartType: LiftMetricChartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX,
): BaseFirestoreDto() {
    override fun copyWithBase(): BaseFirestoreDto {
        return this.copy().apply {
            firestoreId = this@LiftMetricChartFirestoreDto.firestoreId
            lastUpdated = this@LiftMetricChartFirestoreDto.lastUpdated
            synced = this@LiftMetricChartFirestoreDto.synced
        }
    }
}
