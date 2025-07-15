package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.WorkoutLogEntryFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutLogEntriesSyncRepository(
    private val dao: WorkoutLogEntryDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<WorkoutLogEntryFirestoreDto, WorkoutLogEntry>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<WorkoutLogEntryFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}