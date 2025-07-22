package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.SetLogEntryRemoteDto
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.data.sync.FirestoreClient
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
    override val firestoreDocClass: KClass<SetLogEntryRemoteDto> = SetLogEntryRemoteDto::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<SetLogEntryRemoteDto> {
        return setLogEntriesRepository.getManySetLogEntries(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<SetLogEntryRemoteDto> {
        return setLogEntriesRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<SetLogEntryRemoteDto>> {
        return setLogEntriesRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<SetLogEntryRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            setLogEntriesRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<SetLogEntryRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            setLogEntriesRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        return emptyList()
    }
}