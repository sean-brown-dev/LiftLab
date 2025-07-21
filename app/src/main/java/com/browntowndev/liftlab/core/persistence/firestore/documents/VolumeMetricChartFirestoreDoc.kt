package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

@Keep
data class VolumeMetricChartFirestoreDoc(
    override var id: Long = 0L,
    var volumeType: VolumeType = VolumeType.CHEST,
    var volumeTypeImpact: VolumeTypeImpact = VolumeTypeImpact.PRIMARY,
): BaseFirestoreDoc() {
    override fun copyWithBase(): BaseFirestoreDoc {
        return this.copy().apply {
            firestoreId = this@VolumeMetricChartFirestoreDoc.firestoreId
            lastUpdated = this@VolumeMetricChartFirestoreDoc.lastUpdated
            synced = this@VolumeMetricChartFirestoreDoc.synced
        }
    }
}
