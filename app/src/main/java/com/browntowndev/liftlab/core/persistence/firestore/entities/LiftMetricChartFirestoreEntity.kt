package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

@Keep
data class LiftMetricChartFirestoreEntity(
    override var id: Long = 0L,
    var liftId: Long? = null,
    var chartType: LiftMetricChartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX,
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@LiftMetricChartFirestoreEntity.firestoreId
            lastUpdated = this@LiftMetricChartFirestoreEntity.lastUpdated
            synced = this@LiftMetricChartFirestoreEntity.synced
        }
    }
}
