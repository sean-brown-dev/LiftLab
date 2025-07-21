package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogEntriesRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutLogEntryFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreClient
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
    override val firestoreDocClass: KClass<WorkoutLogEntryFirestoreDoc> = WorkoutLogEntryFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<WorkoutLogEntryFirestoreDoc> {
        return workoutLogEntriesRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<WorkoutLogEntryFirestoreDoc> {
        return workoutLogEntriesRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<WorkoutLogEntryFirestoreDoc>> {
        return workoutLogEntriesRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutLogEntryFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutLogEntriesRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutLogEntryFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutLogEntriesRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        return emptyList()
    }
}