package com.browntowndev.liftlab.core.data.remote

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.mapParallel
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.SyncMetadataDto
import com.browntowndev.liftlab.core.data.remote.repositories.RemoteSyncRepository
import com.browntowndev.liftlab.core.domain.repositories.SyncMetadataRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date

class SyncOrchestrator(
    private val syncMetadataRepository: SyncMetadataRepository,
    private val syncRepositories: List<RemoteSyncRepository>,
    private val remoteDataClient: RemoteDataClient,
) {
    companion object {
        private val mutex = Mutex()
    }

    suspend fun syncAll() {
        mutex.withLock {
            downloadNewerChanges()
            uploadPendingChanges()
        }
    }

    private suspend fun downloadNewerChanges() {
        syncRepositories.forEach { syncRepository ->
            val collectionName = syncRepository.collectionName
            val syncMetadata = syncMetadataRepository.get(collectionName = collectionName)
            val lastSynced = syncMetadata?.lastSyncTimestamp ?: Date(0)

            val batchMaxLastUpdatedDates = remoteDataClient.getAllSince(
                collectionName = collectionName, lastUpdated = lastSynced
            ).mapParallel(10) { remoteEntityChunk ->
                updateLocalEntitiesFromRemoteChunk(
                    remoteEntityChunk = remoteEntityChunk,
                    syncRepository = syncRepository,
                )

                remoteEntityChunk.maxOf { it.lastUpdated ?: Date(0) }
            }

            val newLastSynced = maxOf(lastSynced, batchMaxLastUpdatedDates.maxOrNull() ?: Date(0))
            syncMetadataRepository.upsert(
                SyncMetadataDto(
                    collectionName = collectionName,
                    lastSyncTimestamp = newLastSynced
                )
            )
        }
    }

    private suspend fun updateLocalEntitiesFromRemoteChunk(
        remoteEntityChunk: List<BaseRemoteDto>,
        syncRepository: RemoteSyncRepository,
    ) {
        val localEntities = syncRepository.getManyByRemoteId(remoteEntityChunk.mapNotNull { it.remoteId })
            .associateBy { it.remoteId }

        val remoteEntitiesToUpsert = remoteEntityChunk.mapNotNull { remoteEntity ->
            val localEntity = localEntities[remoteEntity.remoteId]
            val localEntityLastUpdated = localEntity?.lastUpdated ?: Date(0)

            if (remoteEntity.lastUpdated?.after(localEntityLastUpdated) == true) {
                remoteEntity
            } else null
        }

        syncRepository.upsertMany(remoteEntitiesToUpsert)
    }

    private suspend fun uploadPendingChanges() {
        syncRepositories.fastForEach { syncRepository ->
            processSyncBatches(syncRepository)
        }
    }

    private suspend fun processSyncBatches(syncRepository: RemoteSyncRepository) {
        val unsyncedEntities = syncRepository.getAllUnsynced()
        if (unsyncedEntities.isEmpty()) return

        val toUpsert = unsyncedEntities.filter { !it.deleted }
        val toDelete = (unsyncedEntities - toUpsert).filter { it.remoteId != null }

        val syncBatches: List<BatchSyncCollection> = buildList {
            if (toUpsert.isNotEmpty()) {
                add(
                    BatchSyncCollection(
                        collectionName = syncRepository.collectionName,
                        remoteEntities = toUpsert,
                        syncType = SyncType.Upsert
                    )
                )
            }
            if (toDelete.isNotEmpty()) {
                add(
                    BatchSyncCollection(
                        collectionName = syncRepository.collectionName,
                        remoteEntities = toDelete,
                        syncType = SyncType.Delete
                    )
                )
            }
        }

        val upsertDocumentIds = remoteDataClient.executeBatchSync(syncBatches)
        if (upsertDocumentIds.isNotEmpty()) {
            val upsertedRemoteDtos = remoteDataClient.getMany(
                collectionName = syncRepository.collectionName,
                ids = upsertDocumentIds
            )
            syncRepository.upsertMany(upsertedRemoteDtos)
        }
        if (toDelete.isNotEmpty()) {
            syncRepository.deleteManyByRemoteId(toDelete.fastMapNotNull { it.remoteId })
        }
    }
}