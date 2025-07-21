package com.browntowndev.liftlab.core.persistence.firestore.entities

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

@Keep
data class VolumeMetricChartFirestoreEntity(
    override var id: Long = 0L,
    var volumeType: VolumeType = VolumeType.CHEST,
    var volumeTypeImpact: VolumeTypeImpact = VolumeTypeImpact.PRIMARY,
): BaseFirestoreEntity() {
    override fun copyWithBase(): BaseFirestoreEntity {
        return this.copy().apply {
            firestoreId = this@VolumeMetricChartFirestoreEntity.firestoreId
            lastUpdated = this@VolumeMetricChartFirestoreEntity.lastUpdated
            synced = this@VolumeMetricChartFirestoreEntity.synced
        }
    }
}
