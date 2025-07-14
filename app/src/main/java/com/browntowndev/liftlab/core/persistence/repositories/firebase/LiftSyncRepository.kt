package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.LiftFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.FirebaseFirestore

class LiftSyncRepository(
    private val dao: LiftsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<LiftFirebaseDto, Lift>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "lifts",
    userId = userId
) {
    override suspend fun getAll(): List<LiftFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}
