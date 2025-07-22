package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.data.remote.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.HistoricalWorkoutNameRemoteDto
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
    override val firestoreDocClass: KClass<HistoricalWorkoutNameRemoteDto> = HistoricalWorkoutNameRemoteDto::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<HistoricalWorkoutNameRemoteDto> {
        return historicalWorkoutNamesRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<HistoricalWorkoutNameRemoteDto> {
        return historicalWorkoutNamesRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<HistoricalWorkoutNameRemoteDto>> {
        return historicalWorkoutNamesRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<HistoricalWorkoutNameRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            historicalWorkoutNamesRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<HistoricalWorkoutNameRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            historicalWorkoutNamesRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        return emptyList()
    }
}