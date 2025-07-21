package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.firestore.documents.LiftMetricChartFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.LiftMetricChartEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LiftMetricChartsSyncRepository(
    private val dao: LiftMetricChartsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<LiftMetricChartFirestoreDoc, LiftMetricChartEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<LiftMetricChartFirestoreDoc> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<LiftMetricChartFirestoreDoc> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}