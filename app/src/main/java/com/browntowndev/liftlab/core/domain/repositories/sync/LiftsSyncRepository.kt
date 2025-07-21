package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.LiftFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.LiftEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LiftsSyncRepository(
    private val dao: LiftsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<LiftFirestoreEntity, LiftEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.LIFTS_COLLECTION,
    firebaseAuth = firebaseAuth
) {
    override suspend fun getAll(): List<LiftFirestoreEntity> =
        dao.getAll(includeHidden = true).map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<LiftFirestoreEntity> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}
