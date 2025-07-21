package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutsSyncRepository(
    private val dao: WorkoutsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<WorkoutFirestoreDoc, WorkoutEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutFirestoreDoc> =
        dao.getAll().map { it.toFirestoreDto() }

    fun getAllFlow(): Flow<List<WorkoutFirestoreDoc>> =
        dao.getAllFlow().map { workouts ->
            workouts.map { workout -> workout.toFirestoreDto() }
        }

    override suspend fun getMany(ids: List<Long>): List<WorkoutFirestoreDoc> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}