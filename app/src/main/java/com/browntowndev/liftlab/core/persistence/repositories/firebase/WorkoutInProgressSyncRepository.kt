package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.WorkoutInProgressFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutInProgressSyncRepository(
    private val dao: WorkoutInProgressDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<WorkoutInProgressFirestoreDto, WorkoutInProgress>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutInProgressFirestoreDto> =
        dao.get()?.let { workoutInProgress ->
            listOf(workoutInProgress.toFirestoreDto())
        } ?: emptyList()
}