package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.VolumeMetricChartFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VolumeMetricChartsSyncRepository(
    private val dao: VolumeMetricChartsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<VolumeMetricChartFirestoreEntity, VolumeMetricChartEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<VolumeMetricChartFirestoreEntity> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<VolumeMetricChartFirestoreEntity> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}