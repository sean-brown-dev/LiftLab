package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.WorkoutFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.WorkoutInProgressFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutInProgressBaseSyncRepository(
    private val dao: WorkoutInProgressDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<WorkoutInProgressFirebaseDto, WorkoutInProgress>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "workoutInProgress",
    userId = userId,
) {
    override suspend fun getAll(): List<WorkoutInProgressFirebaseDto> =
        dao.get()?.let { workoutInProgress ->
            listOf(workoutInProgress.toFirebaseDto())
        } ?: emptyList()
}