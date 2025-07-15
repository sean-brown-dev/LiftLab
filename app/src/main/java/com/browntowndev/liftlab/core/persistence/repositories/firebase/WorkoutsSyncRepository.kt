package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.WorkoutFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutsSyncRepository(
    private val dao: WorkoutsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<WorkoutFirestoreDto, Workout>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<WorkoutFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}