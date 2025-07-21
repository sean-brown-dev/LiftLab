package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutInProgressFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.map

class WorkoutInProgressSyncRepository(
    private val dao: WorkoutInProgressDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<WorkoutInProgressFirestoreDoc, WorkoutInProgressEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutInProgressFirestoreDoc> =
        dao.get()?.let { workoutInProgress ->
            listOf(workoutInProgress.toFirestoreDto())
        } ?: emptyList()

    fun getAllFlow(): Flow<List<WorkoutInProgressFirestoreDoc>> =
        dao.getAllFlow().map { workoutInProgress ->
            workoutInProgress.map { workoutInProgress -> workoutInProgress.toFirestoreDto() }
        }

    override suspend fun getMany(ids: List<Long>): List<WorkoutInProgressFirestoreDoc> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}