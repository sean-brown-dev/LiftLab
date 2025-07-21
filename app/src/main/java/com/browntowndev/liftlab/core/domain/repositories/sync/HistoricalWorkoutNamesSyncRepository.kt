package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.firestore.documents.HistoricalWorkoutNameFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistoricalWorkoutNamesSyncRepository(
    private val dao: HistoricalWorkoutNamesDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<HistoricalWorkoutNameFirestoreDoc, HistoricalWorkoutNameEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
    firebaseAuth = firebaseAuth
) {
    override suspend fun getAll(): List<HistoricalWorkoutNameFirestoreDoc> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutNameFirestoreDoc> =
        dao.getMany(ids).map { it.toFirestoreDto() }
}