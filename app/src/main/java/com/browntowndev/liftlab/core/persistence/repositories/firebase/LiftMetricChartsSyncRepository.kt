package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.LiftMetricChartFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.FirebaseFirestore

class LiftMetricChartsSyncRepository(
    private val dao: LiftMetricChartsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<LiftMetricChartFirebaseDto, LiftMetricChart>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = FirebaseConstants.LIFT_METRIC_CHARTS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<LiftMetricChartFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}