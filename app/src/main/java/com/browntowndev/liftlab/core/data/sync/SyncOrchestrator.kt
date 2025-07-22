package com.browntowndev.liftlab.core.data.sync

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.forEachParallel
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.local.dao.SyncMetadataDao
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.sync.repositories.RemoteSyncRepository
import java.util.Date

class SyncOrchestrator(
    private val syncMetadataDao: SyncMetadataDao,
    private val syncRepositories: List<RemoteSyncRepository>,
    private val remoteDataClient: RemoteDataClient,
) {
    suspend fun syncAll() {
        downloadNewerChanges()
        uploadPendingChanges()
    }

    private suspend fun uploadPendingChanges() {
        syncRepositories.fastForEach { syncRepository ->
                val unsyncedEntities = syncRepository.getAllUnsynced()
                if (unsyncedEntities.isNotEmpty()) return@fastForEach

                val toUpsert = unsyncedEntities.filter { !it.deleted }
                val toDelete = unsyncedEntities - toUpsert

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

                remoteDataClient.executeBatchSync(syncBatches)
            }
    }

    private suspend fun downloadNewerChanges() {
        val collectionNames = syncRepositories.forEach { syncRepository ->
            val collectionName = syncRepository.collectionName
            val syncMetadata = syncMetadataDao.getForCollection(collectionName = collectionName)
            val lastSynced = syncMetadata?.lastSyncTimestamp ?: Date(0)

            remoteDataClient.getAllSince(
                collectionName = collectionName, lastUpdated = lastSynced
            ).forEachParallel(10) { remoteEntityChunk ->
                processRemoteEntityChunk(
                    remoteEntityChunk = remoteEntityChunk,
                    syncRepository = syncRepository,
                )
            }
        }
    }

    private suspend fun processRemoteEntityChunk(
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
}