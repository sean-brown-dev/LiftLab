package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.LiftMetricChartFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class LiftMetricChartsSyncRepository(
    private val dao: LiftMetricChartsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<LiftMetricChartFirestoreDto, LiftMetricChart>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<LiftMetricChartFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}