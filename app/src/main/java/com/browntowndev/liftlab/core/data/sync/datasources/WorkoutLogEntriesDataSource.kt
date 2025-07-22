package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutLogEntryRemoteDto
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.data.sync.FirestoreClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class WorkoutLogEntriesDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val workoutLogEntriesRepository: WorkoutLogRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION
    override val firestoreDocClass: KClass<WorkoutLogEntryRemoteDto> = WorkoutLogEntryRemoteDto::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<WorkoutLogEntryRemoteDto> {
        return workoutLogEntriesRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<WorkoutLogEntryRemoteDto> {
        return workoutLogEntriesRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<WorkoutLogEntryRemoteDto>> {
        return workoutLogEntriesRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<WorkoutLogEntryRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutLogEntriesRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<WorkoutLogEntryRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutLogEntriesRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        return emptyList()
    }
}