package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.LiftFirestoreDto
import com.browntowndev.liftlab.core.persistence.dtos.firestore.LiftMetricChartFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LiftsSyncRepository(
    private val dao: LiftsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<LiftFirestoreDto, Lift>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.LIFTS_COLLECTION,
    firebaseAuth = firebaseAuth
) {
    override suspend fun getAll(): List<LiftFirestoreDto> =
        dao.getAll(includeHidden = true).map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<LiftFirestoreDto> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}
