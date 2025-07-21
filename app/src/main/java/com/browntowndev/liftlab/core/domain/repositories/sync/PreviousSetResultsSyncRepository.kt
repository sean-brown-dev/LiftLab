package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.PreviousSetResultFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.PreviousSetResultEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PreviousSetResultsSyncRepository(
    private val dao: PreviousSetResultDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<PreviousSetResultFirestoreEntity, PreviousSetResultEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<PreviousSetResultFirestoreEntity> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<PreviousSetResultFirestoreEntity> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}