package com.browntowndev.liftlab.core.persistence.repositories.firestore

import android.util.Log
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.WorkoutLiftFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutLiftsSyncRepository(
    private val dao: WorkoutLiftsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<WorkoutLiftFirestoreDto, WorkoutLift>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
    firebaseAuth = firebaseAuth,
) {
    override suspend fun getAll(): List<WorkoutLiftFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }

    fun getAllFlow(): Flow<List<WorkoutLiftFirestoreDto>> =
        dao.getAllFlow().map { workoutLifts ->
            workoutLifts.map { workoutLift -> workoutLift.toFirestoreDto() }
        }

    override suspend fun getMany(ids: List<Long>): List<WorkoutLiftFirestoreDto> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}