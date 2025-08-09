package com.browntowndev.liftlab.core.data.remote

import android.util.Log
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.forEachParallel
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.SyncMetadataDto
import com.browntowndev.liftlab.core.data.remote.repositories.RemoteSyncRepository
import com.browntowndev.liftlab.core.domain.repositories.SyncMetadataRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date

class SyncOrchestrator(
    private val syncMetadataRepository: SyncMetadataRepository,
    private val syncRepositories: List<RemoteSyncRepository>,
    private val remoteDataClient: RemoteDataClient,
    private val transactionScope: TransactionScope,
    private val syncHierarchy: List<HashSet<String>>,
) {
    companion object {
        private val mutex = Mutex()
        private const val TAG = "SyncOrchestrator"
    }

    suspend fun syncToRemote() {
        Log.d(TAG, "syncToRemote called")
        if (!remoteDataClient.canSync) {
            Log.d(TAG, "Cannot sync, remoteDataClient.canSync is false")
            return
        }

        try {
            mutex.withLock {
                Log.d(TAG, "Starting sync process")
                transactionScope.execute {
                    uploadPendingChanges()
                }
                Log.d(TAG, "Sync process finished")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    suspend fun syncFromThenToRemote(syncAllFromRemote: Boolean = false) {
        Log.d(TAG, "syncAll called with sync all data: $syncAllFromRemote")

        var canSync = remoteDataClient.canSync
        var retries = 0
        while (retries < 3 && !canSync) {
            Log.d(TAG, "Cannot sync, retrying")
            canSync = remoteDataClient.canSync
            retries++
            delay(100)
        }
        if (!canSync) {
            Log.d(TAG, "Cannot sync, remoteDataClient.canSync is false")
            return
        }

        try {
            mutex.withLock {
                Log.d(TAG, "Starting sync process")
                transactionScope.execute {
                    downloadNewerChanges(syncAll = syncAllFromRemote)
                    uploadPendingChanges()
                }
                Log.d(TAG, "Sync process finished")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    private suspend fun downloadNewerChanges(syncAll: Boolean) {
        Log.d(TAG, "Starting downloadNewerChanges")

        syncHierarchy.executeForEachRepositoryParallel { syncRepository ->
            val collectionName = syncRepository.collectionName
            val lastSynced = if (syncAll) Date(0)
                else syncMetadataRepository.get(collectionName = collectionName)?.lastSyncTimestamp ?: Date(0)
            Log.d(TAG, "Last synced for $collectionName: $lastSynced")
            var latestRemoteLastUpdated = lastSynced

            remoteDataClient.getAllSince(
                collectionName = collectionName, lastUpdated = lastSynced
            ).collect { remoteEntities ->
                if (remoteEntities.isEmpty()) return@collect

                Log.d(
                    TAG,
                    "Processing chunk of ${remoteEntities.size} remote entities for collection $collectionName"
                )
                updateLocalEntitiesFromRemoteChunk(
                    remoteEntityChunk = remoteEntities,
                    syncRepository = syncRepository,
                )
                val latestRemoteLastUpdatedForBatch =
                    remoteEntities.maxOf { it.lastUpdated ?: Date(0) }
                latestRemoteLastUpdated =
                    maxOf(latestRemoteLastUpdated, latestRemoteLastUpdatedForBatch)
            }

            Log.d(
                TAG,
                "Updating sync metadata for $collectionName, newLastSynced: $latestRemoteLastUpdated"
            )
            syncMetadataRepository.upsert(
                SyncMetadataDto(
                    collectionName = collectionName,
                    lastSyncTimestamp = latestRemoteLastUpdated
                )
            )
        }
        Log.d(TAG, "Finished downloadNewerChanges")
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

        if (remoteEntitiesToUpsert.isNotEmpty()) {
            Log.d(TAG, "Upserting ${remoteEntitiesToUpsert.size} remote entities locally for collection ${syncRepository.collectionName}")
            syncRepository.upsertMany(remoteEntitiesToUpsert)
        } else {
            Log.d(TAG, "No remote entities to upsert locally for collection ${syncRepository.collectionName}")
        }
    }

    private suspend fun uploadPendingChanges() {
        Log.d(TAG, "Starting uploadPendingChanges")
        syncHierarchy.executeForEachRepositoryParallel { syncRepository ->
            processUpsertBatches(syncRepository)
        }
        syncHierarchy.asReversed().executeForEachRepositoryParallel { syncRepository ->
            processDeleteBatches(syncRepository)
        }
        Log.d(TAG, "Finished uploadPendingChanges")
    }

    private suspend fun processUpsertBatches(syncRepository: RemoteSyncRepository) {
        Log.d(TAG, "Processing upsert batches for ${syncRepository.collectionName}")

        val unsyncedEntities = syncRepository.getAllUnsynced()
        if (unsyncedEntities.isEmpty()) {
            Log.d(TAG, "No unsynced entities for collection ${syncRepository.collectionName}")
            return
        }

        val toUpsert = unsyncedEntities.fastFilter { !it.deleted }
        if (toUpsert.isEmpty()) {
            Log.d(TAG, "No entities to upsert for collection ${syncRepository.collectionName}")
            return
        }

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
        }

        Log.d(TAG, "Executing upsert batch sync for ${syncRepository.collectionName}: ${syncBatches.size} batches")
        val upsertDocumentIds = remoteDataClient.executeBatchSync(syncBatches)
        if (upsertDocumentIds.isNotEmpty()) {
            Log.d(TAG, "Successfully upserted ${upsertDocumentIds.size} documents for ${syncRepository.collectionName}, fetching updated DTOs")
            remoteDataClient.getMany(
                collectionName = syncRepository.collectionName,
                ids = upsertDocumentIds
            ).filter { it.isNotEmpty() }
            .collect { upsertedRemoteDTOs ->
                if (upsertedRemoteDTOs.isEmpty()) return@collect

                syncRepository.upsertMany(upsertedRemoteDTOs)
                Log.d(TAG, "Successfully updated ${upsertedRemoteDTOs.size} local entities for ${syncRepository.collectionName}")
            }
        }
    }

    private suspend fun processDeleteBatches(syncRepository: RemoteSyncRepository) {
        Log.d(TAG, "Processing delete batches for ${syncRepository.collectionName}")

        val unsyncedEntities = syncRepository.getAllUnsynced()
        if (unsyncedEntities.isEmpty()) {
            Log.d(TAG, "No unsynced entities for collection ${syncRepository.collectionName}")
            return
        }

        val (toDelete, deletedWithoutFirestoreId) = unsyncedEntities.fastFilter { it.deleted }.partition { it.remoteId != null }

        if (deletedWithoutFirestoreId.isNotEmpty()) {
            // Doing this so they don't keep re-trying to delete fruitlessly
            Log.d(TAG, "Marking ${deletedWithoutFirestoreId.size} entities that are deleted without a remoteId as synced for collection ${syncRepository.collectionName}")
            val toUpsertAsSynced = deletedWithoutFirestoreId.fastMap {
                it.copyWithBase().apply {
                    synced = true
                }
            }
            syncRepository.upsertMany(toUpsertAsSynced)
        }
        if (toDelete.isEmpty()) {
            Log.d(TAG, "No entities to delete for collection ${syncRepository.collectionName}")
            return
        }

        val syncBatches: List<BatchSyncCollection> = buildList {
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

        Log.d(TAG, "Executing batch delete for ${syncRepository.collectionName}: ${syncBatches.size} batches")
        remoteDataClient.executeBatchSync(syncBatches)

        Log.d(TAG, "Deleting ${toDelete.size} entities locally for ${syncRepository.collectionName}")
        syncRepository.deleteManyByRemoteId(toDelete.fastMapNotNull { it.remoteId })
    }

    private suspend fun List<HashSet<String>>.executeForEachRepositoryParallel(
        action: suspend (syncRepository: RemoteSyncRepository
    ) -> Unit) {
        fastForEach { collectionNames ->
            syncRepositories
                .filter { it.collectionName in collectionNames }
                .forEachParallel { syncRepository ->
                    action(syncRepository)
                }
        }
    }
}