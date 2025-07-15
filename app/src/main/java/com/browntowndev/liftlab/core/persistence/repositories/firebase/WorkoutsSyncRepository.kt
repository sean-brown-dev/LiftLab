package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.WorkoutFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutsSyncRepository(
    private val dao: WorkoutsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<WorkoutFirebaseDto, Workout>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = FirebaseConstants.WORKOUTS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<WorkoutFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}