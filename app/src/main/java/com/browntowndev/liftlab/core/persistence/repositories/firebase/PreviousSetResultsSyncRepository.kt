package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.PreviousSetResultFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class PreviousSetResultsSyncRepository(
    private val dao: PreviousSetResultDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<PreviousSetResultFirestoreDto, PreviousSetResult>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<PreviousSetResultFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}