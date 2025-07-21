package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.firestore.documents.LiftFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.LiftEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LiftsSyncRepository(
    private val dao: LiftsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<LiftFirestoreDoc, LiftEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.LIFTS_COLLECTION,
    firebaseAuth = firebaseAuth
) {
    override suspend fun getAll(): List<LiftFirestoreDoc> =
        dao.getAll(includeHidden = true).map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<LiftFirestoreDoc> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}
