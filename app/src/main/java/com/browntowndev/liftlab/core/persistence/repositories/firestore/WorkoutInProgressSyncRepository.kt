package com.browntowndev.liftlab.core.persistence.repositories.firestore

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.WorkoutInProgressFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.map

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

    fun getAllFlow(): Flow<List<WorkoutInProgressFirestoreDto>> =
        dao.getAllFlow().map { workoutInProgress ->
            workoutInProgress.map { workoutInProgress -> workoutInProgress.toFirestoreDto() }
        }

    override suspend fun getMany(ids: List<Long>): List<WorkoutInProgressFirestoreDto> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}