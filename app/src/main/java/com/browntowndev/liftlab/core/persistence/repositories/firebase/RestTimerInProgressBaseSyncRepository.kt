package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.ProgramFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.RestTimerInProgressFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class RestTimerInProgressBaseSyncRepository(
    private val dao: RestTimerInProgressDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<RestTimerInProgressFirebaseDto, RestTimerInProgress>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "restTimerInProgress",
    userId = userId,
) {
    override suspend fun getAll(): List<RestTimerInProgressFirebaseDto> =
        dao.get()?.let { restTimerInProgress ->
            listOf(restTimerInProgress.toFirebaseDto())
        } ?: emptyList()
}