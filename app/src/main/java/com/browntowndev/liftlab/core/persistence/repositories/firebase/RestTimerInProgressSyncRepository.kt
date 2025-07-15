package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.RestTimerInProgressFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class RestTimerInProgressSyncRepository(
    private val dao: RestTimerInProgressDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<RestTimerInProgressFirestoreDto, RestTimerInProgress>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.REST_TIMER_IN_PROGRESS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<RestTimerInProgressFirestoreDto> =
        dao.get()?.let { restTimerInProgress ->
            listOf(restTimerInProgress.toFirestoreDto())
        } ?: emptyList()
}