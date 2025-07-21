package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.SetLogEntryFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class SetLogEntriesDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val setLogEntriesRepository: SetLogEntryRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION
    override val firestoreDocClass: KClass<SetLogEntryFirestoreDoc> = SetLogEntryFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<SetLogEntryFirestoreDoc> {
        return setLogEntriesRepository.getManySetLogEntries(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<SetLogEntryFirestoreDoc> {
        return setLogEntriesRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<SetLogEntryFirestoreDoc>> {
        return setLogEntriesRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<SetLogEntryFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            setLogEntriesRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<SetLogEntryFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            setLogEntriesRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        return emptyList()
    }
}