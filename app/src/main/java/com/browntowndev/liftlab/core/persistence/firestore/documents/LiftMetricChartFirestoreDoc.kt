package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

@Keep
data class LiftMetricChartFirestoreDoc(
    override var id: Long = 0L,
    var liftId: Long? = null,
    var chartType: LiftMetricChartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX,
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@LiftMetricChartFirestoreDoc.firestoreId
            lastUpdated = this@LiftMetricChartFirestoreDoc.lastUpdated
            synced = this@LiftMetricChartFirestoreDoc.synced
        }
    }
}
