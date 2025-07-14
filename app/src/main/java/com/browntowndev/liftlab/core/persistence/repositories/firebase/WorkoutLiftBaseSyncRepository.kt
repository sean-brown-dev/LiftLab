package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.WorkoutInProgressFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.WorkoutLiftFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutLiftBaseSyncRepository(
    private val dao: WorkoutLiftsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<WorkoutLiftFirebaseDto, WorkoutLift>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "workoutLifts",
    userId = userId,
) {
    override suspend fun getAll(): List<WorkoutLiftFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}