package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.VolumeMetricChartFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.FirebaseFirestore

class VolumeMetricChartsSyncRepository(
    private val dao: VolumeMetricChartsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<VolumeMetricChartFirebaseDto, VolumeMetricChart>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = FirebaseConstants.VOLUME_METRIC_CHARTS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<VolumeMetricChartFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}