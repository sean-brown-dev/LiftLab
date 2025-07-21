package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutLiftFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutLiftEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutLiftsSyncRepository(
    private val dao: WorkoutLiftsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<WorkoutLiftFirestoreEntity, WorkoutLiftEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutLiftFirestoreEntity> =
        dao.getAll().map { it.toFirestoreDto() }

    fun getAllFlow(): Flow<List<WorkoutLiftFirestoreEntity>> =
        dao.getAllFlow().map { workoutLifts ->
            workoutLifts.map { workoutLift -> workoutLift.toFirestoreDto() }
        }

    override suspend fun getMany(ids: List<Long>): List<WorkoutLiftFirestoreEntity> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}