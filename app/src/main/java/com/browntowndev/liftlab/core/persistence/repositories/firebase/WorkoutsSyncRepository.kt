package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.WorkoutFirestoreDto
import com.browntowndev.liftlab.core.persistence.dtos.firestore.WorkoutLogEntryFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutsSyncRepository(
    private val dao: WorkoutsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<WorkoutFirestoreDto, Workout>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }

    fun getAllFlow(): Flow<List<WorkoutFirestoreDto>> =
        dao.getAllFlow().map { workouts ->
            workouts.map { workout -> workout.toFirestoreDto() }
        }

    override suspend fun getMany(ids: List<Long>): List<WorkoutFirestoreDto> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}