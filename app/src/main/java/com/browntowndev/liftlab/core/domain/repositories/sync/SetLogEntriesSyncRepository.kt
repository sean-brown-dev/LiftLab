package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.SetLogEntryFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.SetLogEntryEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SetLogEntriesSyncRepository(
    private val dao: SetLogEntryDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<SetLogEntryFirestoreEntity, SetLogEntryEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<SetLogEntryFirestoreEntity> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<SetLogEntryFirestoreEntity> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}