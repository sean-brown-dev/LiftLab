package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.LiftMetricChartFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.PreviousSetResultFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class PreviousSetResultBaseSyncRepository(
    private val dao: PreviousSetResultDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<PreviousSetResultFirebaseDto, PreviousSetResult>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "previousSetResults",
    userId = userId,
) {
    override suspend fun getAll(): List<PreviousSetResultFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}