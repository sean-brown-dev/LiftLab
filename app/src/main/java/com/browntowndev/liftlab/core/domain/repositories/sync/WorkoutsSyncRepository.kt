package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutEntity
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
) : BaseSyncRepository<WorkoutFirestoreEntity, WorkoutEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutFirestoreEntity> =
        dao.getAll().map { it.toFirestoreDto() }

    fun getAllFlow(): Flow<List<WorkoutFirestoreEntity>> =
        dao.getAllFlow().map { workouts ->
            workouts.map { workout -> workout.toFirestoreDto() }
        }

    override suspend fun getMany(ids: List<Long>): List<WorkoutFirestoreEntity> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}