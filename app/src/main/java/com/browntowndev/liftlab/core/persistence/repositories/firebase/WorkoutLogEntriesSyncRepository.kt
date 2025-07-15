package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.WorkoutLogEntryFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutLogEntriesSyncRepository(
    private val dao: WorkoutLogEntryDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<WorkoutLogEntryFirebaseDto, WorkoutLogEntry>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = FirebaseConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<WorkoutLogEntryFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}