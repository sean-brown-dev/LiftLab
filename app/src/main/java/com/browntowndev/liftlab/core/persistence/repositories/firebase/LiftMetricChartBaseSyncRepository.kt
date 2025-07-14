package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.LiftFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.LiftMetricChartFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class LiftMetricChartBaseSyncRepository(
    private val dao: LiftMetricChartsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<LiftMetricChartFirebaseDto, LiftMetricChart>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "liftMetricCharts",
    userId = userId,
) {
    override suspend fun getAll(): List<LiftMetricChartFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}