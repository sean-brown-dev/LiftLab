package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.VolumeMetricChartFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VolumeMetricChartsSyncRepository(
    private val dao: VolumeMetricChartsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<VolumeMetricChartFirestoreDto, VolumeMetricChart>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<VolumeMetricChartFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}