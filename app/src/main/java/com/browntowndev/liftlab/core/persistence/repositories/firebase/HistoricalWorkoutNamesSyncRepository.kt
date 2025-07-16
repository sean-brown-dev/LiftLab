package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.HistoricalWorkoutNameFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistoricalWorkoutNamesSyncRepository(
    private val dao: HistoricalWorkoutNamesDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<HistoricalWorkoutNameFirestoreDto, HistoricalWorkoutName>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
    firebaseAuth = firebaseAuth
) {
    override suspend fun getAll(): List<HistoricalWorkoutNameFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}