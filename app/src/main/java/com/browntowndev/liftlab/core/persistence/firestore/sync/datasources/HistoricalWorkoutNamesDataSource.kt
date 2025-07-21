package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.HistoricalWorkoutNameFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class HistoricalWorkoutNamesDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION
    override val firestoreDocClass: KClass<HistoricalWorkoutNameFirestoreDoc> = HistoricalWorkoutNameFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<HistoricalWorkoutNameFirestoreDoc> {
        return historicalWorkoutNamesRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<HistoricalWorkoutNameFirestoreDoc> {
        return historicalWorkoutNamesRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<HistoricalWorkoutNameFirestoreDoc>> {
        return historicalWorkoutNamesRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<HistoricalWorkoutNameFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            historicalWorkoutNamesRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<HistoricalWorkoutNameFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            historicalWorkoutNamesRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        return emptyList()
    }
}