package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutLogEntryFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutLogEntriesSyncRepository(
    private val dao: WorkoutLogEntryDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<WorkoutLogEntryFirestoreDoc, WorkoutLogEntryEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutLogEntryFirestoreDoc> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<WorkoutLogEntryFirestoreDoc> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}