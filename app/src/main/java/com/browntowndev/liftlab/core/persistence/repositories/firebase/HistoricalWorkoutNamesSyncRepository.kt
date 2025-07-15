package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.HistoricalWorkoutNameFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.FirebaseFirestore

class HistoricalWorkoutNamesSyncRepository(
    private val dao: HistoricalWorkoutNamesDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<HistoricalWorkoutNameFirebaseDto, HistoricalWorkoutName>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = FirebaseConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
    userId = userId
) {
    override suspend fun getAll(): List<HistoricalWorkoutNameFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}