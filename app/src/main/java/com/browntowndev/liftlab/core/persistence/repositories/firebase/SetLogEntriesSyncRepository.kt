package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.SetLogEntryFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SetLogEntriesSyncRepository(
    private val dao: SetLogEntryDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<SetLogEntryFirestoreDto, SetLogEntry>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<SetLogEntryFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}