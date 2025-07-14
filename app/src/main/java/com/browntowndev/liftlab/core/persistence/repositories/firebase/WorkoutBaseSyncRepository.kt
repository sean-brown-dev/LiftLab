package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.VolumeMetricChartFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.WorkoutFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutBaseSyncRepository(
    private val dao: WorkoutsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<WorkoutFirebaseDto, Workout>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "workouts",
    userId = userId,
) {
    override suspend fun getAll(): List<WorkoutFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}