package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.LiftMetricChartFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.LiftMetricChartEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LiftMetricChartsSyncRepository(
    private val dao: LiftMetricChartsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<LiftMetricChartFirestoreEntity, LiftMetricChartEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<LiftMetricChartFirestoreEntity> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<LiftMetricChartFirestoreEntity> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}