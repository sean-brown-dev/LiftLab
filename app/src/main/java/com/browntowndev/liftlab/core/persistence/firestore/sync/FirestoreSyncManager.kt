package com.browntowndev.liftlab.core.persistence.firestore.sync

import android.util.Log
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.copyForUpload
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.common.flatMapParallel
import com.browntowndev.liftlab.core.common.forEachParallel
import com.browntowndev.liftlab.core.domain.repositories.SyncMetadataRepository
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.SyncMetadataDto
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class FirestoreSyncManager (
    private val firestoreClient: FirestoreClient,
    private val syncScope: CoroutineScope,
    private val dataSources: Map<String, SyncableDataSource>,
    private val syncRepository: SyncMetadataRepository,
) {
    private val userId: String? get() = firestoreClient.userId

    companion object {
        private const val TAG = "FirestoreSyncManager"
        private const val BATCH_SIZE = 400 // Stay under 500 to be safe

        private val deletionWatcherJobs: MutableMap<String, Job> = ConcurrentHashMap()

        // Queue every distinct request
        private val syncQueue: MutableMap<String, List<SyncQueueEntry>> = mutableMapOf()

        // Queue for batch syncs
        private val batchSyncQueue: MutableList<BatchSyncQueueEntry> = mutableListOf()

        // One running sync per collection
        private val processingSyncs: MutableMap<String, SyncQueueEntry> = mutableMapOf()

        // Batch related sync operations
        private val processingBatchSyncs: MutableList<BatchSyncQueueEntry> = mutableListOf()

        private val processingBatchCollections
            get() = processingBatchSyncs.fastFlatMap { batchQueueEntry -> batchQueueEntry.batch.fastMap { it.collectionName } }.toHashSet()

        private val mutex: Mutex = Mutex()

        private fun isCollectionAlreadyProcessing(batch: BatchSyncQueueEntry): Boolean {
            val batchEntryCollectionNames = batch.batch.map { it.collectionName }
            return batchEntryCollectionNames.any { collectionName ->
                collectionName in processingBatchCollections
            }
        }

        private fun isCollectionAlreadyProcessing(collectionName: String): Boolean {
            return collectionName in processingSyncs
        }
    }

    fun enqueueSyncRequest(entry: SyncQueueEntry) = syncScope.fireAndForgetSync {
        Log.d(TAG, "Received sync request: $entry")
        val shouldStart = mutex.withLock {
            val isAlreadyProcessing = isCollectionAlreadyProcessing(collectionName = entry.collectionName)
            if (!isAlreadyProcessing) {
                processingSyncs.put(entry.collectionName, entry)
            } else {
                val entriesForCollection = syncQueue.getOrElse(entry.collectionName) { listOf() }.toMutableList()
                entriesForCollection.add(entry)
                syncQueue.put(entry.collectionName, entriesForCollection.distinct())
            }

            !isAlreadyProcessing
        }
        Log.d(TAG, "shouldStart=$shouldStart")
        if (shouldStart) {
            try {
                processQueue(entry)
            } catch(e: Exception) {
                // Super crazy error for this to happen. Only place it could happen is trying
                // to dequeue and add next to processing
                processingSyncs.clear()
                syncQueue.clear()

                Log.e(TAG, "Error during processQueue. Dequeueing all: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private suspend fun processQueue(queueEntry: SyncQueueEntry) {
        var entryToProcess: SyncQueueEntry? = queueEntry.copy()

        while (entryToProcess != null) {
            try {
                val waitForBatchQueue = mutex.withLock {
                    entryToProcess.collectionName in processingBatchCollections
                }
                if (waitForBatchQueue) {
                    Log.d(TAG, "Waiting for batch queue")
                    delay(50)
                    continue
                }

                Log.d(TAG, "Processing queue for ${entryToProcess.collectionName}")
                executeSyncRequest(entryToProcess = entryToProcess)
            } catch (e: Exception) {
                Log.e(TAG, "Error during processQueue: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            } finally {
                val nextEntry = mutex.withLock {
                    val entriesForCollection = syncQueue[entryToProcess.collectionName]
                    val nextEntryForCollection = entriesForCollection?.firstOrNull()
                    val hasMoreWork = nextEntryForCollection != null
                    Log.d(TAG, "Next entry for ${entryToProcess.collectionName}: $nextEntryForCollection")

                    if (!hasMoreWork) {
                        processingSyncs.remove(entryToProcess.collectionName)
                        Log.d(TAG, "Processing: $processingSyncs")
                    }
                    else {
                        syncQueue[entryToProcess.collectionName] = entriesForCollection.drop(1)
                        Log.d(TAG, "Queue: $syncQueue")
                    }

                    nextEntryForCollection
                }

                entryToProcess = nextEntry
            }
        }
    }

    private suspend fun executeSyncRequest(
        entryToProcess: SyncQueueEntry,
    ) {
        val dataSource = dataSources[entryToProcess.collectionName] ?: run {
            Log.e(TAG, "No data source found for collection: ${entryToProcess.collectionName}")
            return
        }

        executeSyncRequest(
            collectionName = dataSource.collectionName,
            entities = dataSource.getMany(entryToProcess.roomEntityIds),
            syncType = entryToProcess.syncType,
            onSynced = { entities -> dataSource.upsertMany(entities) }
        )
    }

    private suspend fun executeSyncRequest(
        collectionName: String,
        entities: List<BaseFirestoreDoc>,
        syncType: SyncType,
        onSynced: suspend (List<BaseFirestoreDoc>) -> Unit,
    ) {
        try {
            if (entities.size == 1 && syncType == SyncType.Upsert) {
                syncSingle(
                    collectionName = collectionName,
                    entity = entities[0],
                    onSynced = {
                        onSynced(listOf(it))
                    }
                )
            } else if (entities.size > 1 && syncType == SyncType.Upsert) {
                syncMany(
                    collectionName = collectionName,
                    entities = entities,
                    onSynced = onSynced
                )
            } else if (entities.size == 1 && syncType == SyncType.Delete && entities[0].firestoreId != null) {
                deleteSingle(
                    collectionName = collectionName,
                    firestoreId = entities[0].firestoreId!!
                )
            } else if (entities.size > 1 && syncType == SyncType.Delete) {
                deleteMany(
                    collectionName = collectionName,
                    firestoreIds = entities.mapNotNull { it.firestoreId }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during processQueueEntry: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun enqueueBatchSyncRequest(batch: BatchSyncQueueEntry) = syncScope.fireAndForgetSync {
        Log.d(TAG, "Received batch sync request: $batch")
        val shouldStart = mutex.withLock {
            val isAlreadyProcessing = isCollectionAlreadyProcessing(batch)
            if (!isAlreadyProcessing) {
                processingBatchSyncs.add(batch)
            } else {
                batchSyncQueue.add(batch)
            }

            !isAlreadyProcessing
        }

        Log.d(TAG, "shouldStart=$shouldStart")
        if (shouldStart) {
            try {
                processBatchQueue(batch)
            } catch (e: Exception) {
                // Super crazy error for this to happen. Only place it could happen is trying
                // to dequeue and add next to processing
                processingBatchSyncs.clear()
                batchSyncQueue.clear()

                Log.e(TAG, "Error during processBatchQueue. Dequeueing all: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            } finally {
                Log.d(TAG, "Completed processing batch queue.")
            }
        }
    }

    private suspend fun processBatchQueue(batch: BatchSyncQueueEntry) {
        var batchToProcess: BatchSyncQueueEntry? = batch
        while (batchToProcess != null) {
            try {
                val waitForNonBatchQueue = mutex.withLock {
                    batchToProcess.batch.any { it.collectionName in processingSyncs }
                }
                if (waitForNonBatchQueue) {
                    Log.d(TAG, "Waiting for non-batch queue")
                    delay(50)
                    continue
                }

                Log.d(TAG, "Processing batch queue: $batchToProcess")
                executeBatchSyncRequest(batchToProcess)
            } catch (e: Exception) {
                Log.e(TAG, "Error during processBatchQueue: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            } finally {
                val nextBatch = mutex.withLock {
                    val nextBatch = batchSyncQueue.firstOrNull()
                    val hasMoreWork = nextBatch != null
                    Log.d(TAG, "Next batch: $nextBatch")

                    // Remove current from processing
                    val processingWithoutCurrent = processingBatchSyncs.filter { it.id != batchToProcess.id }
                    processingBatchSyncs.clear()
                    processingBatchSyncs.addAll(processingWithoutCurrent)
                    Log.d(TAG, "Processing: $processingBatchSyncs")

                    if (hasMoreWork) {
                        val queueWithoutNext = batchSyncQueue.filter { it.id != nextBatch.id }
                        batchSyncQueue.clear()
                        batchSyncQueue.addAll(queueWithoutNext)
                        processingBatchSyncs.add(nextBatch)
                        Log.d(TAG, "Queue: $batchSyncQueue")
                    }

                    nextBatch
                }

                batchToProcess = nextBatch
            }
        }
    }

    private suspend fun executeBatchSyncRequest(batch: BatchSyncQueueEntry) {
        val batchSyncCollections = batch.batch.fastMap { entry ->
            BatchSyncCollection(
                collectionName = entry.collectionName,
                roomEntityIds = entry.roomEntityIds,
                syncType = entry.syncType,
            )
        }
        executeBatchSyncRequest(
            batchSyncCollections = batchSyncCollections,
            onRequestEntities = { collectionName, roomEntityIds ->
                dataSources[collectionName]?.getMany(roomEntityIds) ?: listOf()
            },
            onSynced = { collectionName, entities ->
                dataSources[collectionName]?.upsertMany(entities)
            },
            onRequestObjectConversion = { collectionName, documentSnapshot ->
                val dataSource = dataSources[collectionName]
                dataSource?.let {
                    documentSnapshot.toObject(it.firestoreDocClass.java)
                }
            },
        )
    }

    private suspend fun executeBatchSyncRequest(
        batchSyncCollections: List<BatchSyncCollection>,
        onRequestEntities: suspend (collectionName: String, roomEntityIds: List<Long>) -> List<BaseFirestoreDoc>,
        onSynced: suspend (collectionName: String, entities: List<BaseFirestoreDoc>) -> Unit,
        onRequestObjectConversion: (collectionName: String, documentSnapshot: DocumentSnapshot) -> BaseFirestoreDoc?,
    ) {
        val deletedIds = mutableListOf<String>()
        val batchedSyncDocuments = mutableMapOf<String, List<DocumentReference>>()
        val firestoreBatch = firestoreClient.batch()

        batchSyncCollections.fastForEach { batch ->
            val collection = firestoreClient.userCollection(batch.collectionName)

            val batchEntities = onRequestEntities(batch.collectionName, batch.roomEntityIds)
            when (batch.syncType) {
                SyncType.Upsert -> {
                    batchEntities.fastForEach { entity ->
                        val docRef =
                            if (entity.firestoreId != null) collection.document(entity.firestoreId!!)
                            else collection.document()
                        firestoreBatch.set(docRef, entity.copyForUpload(docRef.id))

                        val currentDocRefs = batchedSyncDocuments.getOrDefault(batch.collectionName, listOf()).toMutableList()
                        currentDocRefs.add(docRef)
                        batchedSyncDocuments[batch.collectionName] = currentDocRefs
                    }
                }

                SyncType.Delete -> {
                    batchEntities.fastForEach { entity ->
                        if (entity.firestoreId == null) return@fastForEach
                        val docRef = collection.document(entity.firestoreId!!)
                        firestoreBatch.delete(docRef)
                        deletedIds.add(entity.firestoreId!!)
                    }
                }
            }
        }

        firestoreBatch.commit().await()
        Log.d(TAG, "Batch delete complete: $deletedIds")

        batchedSyncDocuments.forEach { (collectionName, docRefs) ->
            val firestoreEntities = docRefs.fastMapNotNull { docRef ->
                val snapshot = docRef.get().await()
                if (snapshot == null) return@fastMapNotNull null
                onRequestObjectConversion(collectionName, snapshot)
            }
            onSynced(collectionName, firestoreEntities)
            Log.d(TAG, "Batch sync complete: $firestoreEntities [$collectionName]")
        }
    }

    fun tryStartDeletionWatchers() {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping batch delete.")
            return
        }

        dataSources.values.forEach { dataSource ->
            tryStartDeletionWatcher(
                collectionName = dataSource.collectionName,
                entityFlow = dataSource.getAllFlow(),
                onGetFirestoreIdsForEntitiesWithDeletedParents = { entities ->
                    dataSource.getFirestoreIdsForEntitiesWithDeletedParents(entities)
                }
            )
        }
    }

    private fun<T: BaseFirestoreDoc> tryStartDeletionWatcher(
        collectionName: String,
        entityFlow: Flow<List<T>>,
        onGetFirestoreIdsForEntitiesWithDeletedParents: suspend (List<T>) -> List<String>,
    ) {
        val existingJob = deletionWatcherJobs[collectionName]
        if (existingJob?.isActive == true) {
            Log.d(TAG, "Deletion watcher for $collectionName already active.")
            return
        }

        val job = syncScope.fireAndForgetSync {
            val knownEntities = mutableListOf<T>()
            entityFlow.collect { currentDtos ->
                try {
                    Log.d(TAG, "Deletion watcher triggered: $currentDtos [$collectionName]")
                    val currentEntities = currentDtos.mapNotNull { dto ->
                        dto.firestoreId?.let { firestoreId ->
                            firestoreId to dto
                        }
                    }.toMap()
                    val deletedEntities = knownEntities.filter { it.firestoreId !in currentEntities }
                    val idsForDelete = onGetFirestoreIdsForEntitiesWithDeletedParents(deletedEntities)

                    if (idsForDelete.isNotEmpty()) {
                        deleteMany(collectionName, idsForDelete.map { it })
                        Log.d(TAG, "Deleted $idsForDelete")
                    }

                    knownEntities.clear()
                    knownEntities.addAll(currentEntities.values)
                    Log.d(TAG, "Known ids: $knownEntities [$collectionName]")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during deletion watcher for $collectionName: ${e.message}", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        deletionWatcherJobs[collectionName] = job
    }

    private suspend fun deleteSingle(
        collectionName: String,
        firestoreId: String,
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        try {
            val docRef = firestoreClient.userCollection(collectionName)
                .document(firestoreId)

            docRef.delete().await()
            Log.d(TAG, "Deleted entity $firestoreId [$collectionName]")
            FirebaseCrashlytics.getInstance().log("Deleted entity $firestoreId [$collectionName]")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore delete failed: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            // TODO: Add to retry queue if needed
        }
    }

    private suspend fun deleteMany(
        collectionName: String,
        firestoreIds: List<String>,
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping batch delete.")
            return
        }

        if (firestoreIds.isEmpty()) return

        firestoreIds.forEachParallel(10) { chunk ->
            try {
                val batch = firestoreClient.batch()
                val deletedIds = mutableListOf<String>()

                chunk.forEach { firestoreId ->
                    val docRef = firestoreClient.userCollection(collectionName)
                        .document(firestoreId)

                    batch.delete(docRef)
                    deletedIds.add(firestoreId)
                }

                batch.commit().await()
                Log.d(TAG, "Batch deleted ${deletedIds.size} entities [$collectionName]")
                FirebaseCrashlytics.getInstance()
                    .log("Batch deleted ${deletedIds.size} entities [$collectionName]")
            } catch (e: Exception) {
                Log.e(TAG, "Firestore batch delete failed: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                // TODO: Add retry queue
            }
        }
    }

    private suspend inline fun <reified T : BaseFirestoreDoc> syncSingle(
        collectionName: String,
        entity: T,
        noinline onSynced: suspend (firestoreEntity: T) -> Unit
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        try {
            val collection = firestoreClient.userCollection(collectionName)
            val docRef = if (entity.firestoreId != null)
                collection.document(entity.firestoreId!!)
            else
                collection.document()

            val toUpload = entity.copyForUpload(docRef.id)

            docRef.set(toUpload).await()
            val snapshot = docRef.get().await()
            val firestoreEntity = snapshot.toObject<T>()
            if (firestoreEntity != null) {
                onSynced(firestoreEntity)
                Log.d(TAG, "Synced entity ${firestoreEntity.firestoreId}, roomId=${firestoreEntity.id} [$collectionName]")
                FirebaseCrashlytics.getInstance()
                    .log("Synced entity ${firestoreEntity.firestoreId}, roomId=${firestoreEntity.id} [$collectionName]")
            } else {
                Log.e(TAG, "Firestore entity not found after sync [$collectionName]")
                FirebaseCrashlytics.getInstance()
                    .log("Firestore entity not found after sync [$collectionName]")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync failed: ${e.message}  [$collectionName]", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private suspend fun syncMany(
        collectionName: String,
        entities: List<BaseFirestoreDoc>,
        onSynced: suspend (List<BaseFirestoreDoc>) -> Unit,
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        coroutineScope {
            entities.forEachParallel(10) { entityChunk ->
                try {
                    val batch = firestoreClient.batch()
                    val firestoreDocumentBatch = mutableListOf<DocumentReference>()
                    entityChunk.fastForEach { entity ->
                        val collection = firestoreClient.userCollection(collectionName)
                        val docRef = if (entity.firestoreId != null)
                            collection.document(entity.firestoreId!!)
                        else
                            collection.document()

                        val toUpload = entity.copyForUpload(docRef.id)
                        batch.set(docRef, toUpload)
                        firestoreDocumentBatch.add(docRef)
                    }

                    commitBatchAndUpdate(
                        batch = batch,
                        batchSize = firestoreDocumentBatch.size,
                        collectionName = collectionName,
                        currFirestoreDocBatch = firestoreDocumentBatch,
                        onUpdateMany = onSynced
                    )
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    //TODO: add to retry queue
                }
            }
        }
    }

    suspend fun syncAll() = coroutineScope {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return@coroutineScope
        }
        Log.d(TAG, "User $userId logged in. Starting initial sync.")

        val syncLevels = listOf(
            // Level 0: No dependencies
            listOf(
                FirestoreConstants.LIFTS_COLLECTION,
                FirestoreConstants.PROGRAMS_COLLECTION,
                FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION
            ),
            // Level 1: Depends on Level 0
            listOf(
                FirestoreConstants.WORKOUTS_COLLECTION,
                FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION
            ),
            // Level 2: Depends on Level 1
            listOf(
                FirestoreConstants.WORKOUT_LIFTS_COLLECTION,
                FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
                FirestoreConstants.WORKOUT_LIFTS_COLLECTION
            ),
            // Level 3: Depends on Level 2
            listOf(
                FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
                FirestoreConstants.SET_LOG_ENTRIES_COLLECTION
            ),
        )

        try {
            syncLevels.fastMap { level ->
                level.map { collectionName ->
                    async {
                        val dataSource = dataSources[collectionName]
                        if (dataSource != null) {
                            @Suppress("UNCHECKED_CAST")
                            syncEntities(
                                collection = dataSource.collection,
                                lastSyncDate = syncRepository.get(dataSource.collectionName)?.lastSyncTimestamp
                                    ?: Date(0),
                                onGetLocalEntities = { dataSource.getAll() },
                                onUpdateMany = { entities -> dataSource.updateMany(entities) },
                                onUpsertMany = { entities -> dataSource.upsertMany(entities) }
                            )
                        } else {
                            Log.e(TAG, "No data source found for collection: $collectionName")
                        }
                    }
                }.awaitAll()
            }

            Log.d(TAG, "Sync completed for user $userId.")
            FirebaseCrashlytics.getInstance().log("Sync completed for user $userId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync for user $userId: ${e.message}", e)

            FirebaseCrashlytics.getInstance().log("Sync failed for user $userId")
            FirebaseCrashlytics.getInstance().recordException(e)

            throw SyncFailedException()
        }
    }

    private suspend fun syncEntities(
        collection: CollectionReference,
        lastSyncDate: Date,
        onGetLocalEntities: suspend () -> List<BaseFirestoreDoc>,
        onUpdateMany: suspend (List<BaseFirestoreDoc>) -> Unit,
        onUpsertMany: suspend (List<BaseFirestoreDoc>) -> Unit,
    ) {
        val collectionName = collection.id
        val allSyncedEntities: MutableList<BaseFirestoreDoc> = mutableListOf()

        // Update local entities that are outdated first since cloud wins.
        // Prevents duplicate ids from being uploaded to Firestore since any locals
        // with different/no firestoreId will be updated here.
        allSyncedEntities += updateOutdatedLocalEntities(
            collection = collection,
            lastSyncDate = lastSyncDate,
            localEntities = onGetLocalEntities(),
            onUpsertMany = onUpsertMany,
        )

        // Upsert any unsynced entities up to Firestore
        allSyncedEntities += uploadUnsyncedEntities(
            localEntities = onGetLocalEntities(),
            collection = collection,
            collectionName = collectionName,
            onUpdateMany = onUpdateMany
        )

        // Find the newest timestamp across all synced entities
        val latestTimestamp = allSyncedEntities
            .mapNotNull { it.lastUpdated }
            .maxOrNull()

        // Update the last sync timestamp
        if (latestTimestamp != null) {
            val syncMetadata = SyncMetadataDto(
                collectionName = collectionName,
                lastSyncTimestamp = latestTimestamp
            )
            syncRepository.upsert(syncMetadata)
        } else {
            val maxLastUpdated = getMaxLastUpdatedTimestamp(collection) ?: return
            val syncMetadata =
                SyncMetadataDto(collectionName = collectionName, lastSyncTimestamp = maxLastUpdated)
            syncRepository.upsert(syncMetadata)
        }
    }

    private suspend fun updateOutdatedLocalEntities(
        collection: CollectionReference,
        lastSyncDate: Date,
        localEntities: List<BaseFirestoreDoc>,
        onUpsertMany: suspend (List<BaseFirestoreDoc>) -> Unit,
    ): List<BaseFirestoreDoc> {
        val collectionName = collection.id
        val allSyncedEntities = mutableListOf<BaseFirestoreDoc>()
        var lastVisible: DocumentSnapshot? = null
        var done = false
        val localEntitiesInFirestore = localEntities
            .filter { it.firestoreId != null }
            .associateBy { it.firestoreId!! }

        Log.d(TAG, "Checking for outdated entities. ${localEntitiesInFirestore.size} local entities have firestoreId. [$collectionName]")

        while (!done) {
            val query = collection
                .whereGreaterThanOrEqualTo("lastUpdated", lastSyncDate)
                .orderBy("lastUpdated")
                .limit(BATCH_SIZE.toLong())

            val pagedQuery = if (lastVisible != null) {
                query.startAfter(lastVisible)
            } else {
                query
            }

            val snapshot = pagedQuery.get().await()
            val docs = snapshot.documents

            Log.d(TAG, "Fetched ${docs.size} entities to check for updates [$collectionName]")

            if (docs.isEmpty()) {
                done = true
            } else {
                val currFirestoreBatch = docs.flatMapParallel(10) { docChunk ->
                    docChunk.mapNotNull { doc ->
                        val dataSource = dataSources[collectionName]
                        val firestoreEntity = dataSource?.let { doc.toObject(it.firestoreDocClass.java) }
                        val localEntity = localEntitiesInFirestore[firestoreEntity?.firestoreId]
                        val localLastUpdated = localEntity?.lastUpdated ?: Date(0)
                        if (firestoreEntity != null && firestoreEntity.lastUpdated?.after(localLastUpdated) == true) {
                            Log.d(TAG, "Found outdated entity - firestoreEntity Id=${firestoreEntity.firestoreId}: firestore last updated ${firestoreEntity.lastUpdated}, local last updated $localLastUpdated, local firestoreId ${localEntity?.firestoreId} [$collectionName]")
                            firestoreEntity
                        } else null
                    }
                }

                if (currFirestoreBatch.isNotEmpty()) {
                    onUpsertMany(currFirestoreBatch)
                    Log.d(
                        TAG,
                        "Batch updated ${currFirestoreBatch.size} out-of-date entities from Firestore [$collectionName]"
                    )

                    allSyncedEntities += currFirestoreBatch
                }

                lastVisible = docs.last()
            }
        }

        return allSyncedEntities
    }

    private suspend fun uploadUnsyncedEntities(
        localEntities: List<BaseFirestoreDoc>,
        collection: CollectionReference,
        collectionName: String,
        onUpdateMany: suspend (List<BaseFirestoreDoc>) -> Unit
    ): List<BaseFirestoreDoc> {
        val unsyncedEntities = localEntities.filter { !it.synced }
        if (unsyncedEntities.isEmpty()) return emptyList()

        Log.d(TAG, "Uploading ${unsyncedEntities.size} unsynced entities [$collectionName]")
        FirebaseCrashlytics.getInstance().log("Uploading ${unsyncedEntities.size} unsynced entities [$collectionName]")

        val syncedEntities = coroutineScope {
            unsyncedEntities.flatMapParallel(10) { unsyncedEntityChunk ->
                val currFirestoreDocBatch = mutableListOf<DocumentReference>()
                val batch = firestoreClient.batch()

                unsyncedEntityChunk.forEach { unsyncedEntity ->
                    val docRef = unsyncedEntity.firestoreId?.let(collection::document)
                        ?: collection.document()

                    unsyncedEntity.copyForUpload(docRef.id)
                    batch.set(docRef, unsyncedEntity)

                    currFirestoreDocBatch.add(docRef)
                    Log.d(TAG, "Adding entity to batch. firestoreId=${unsyncedEntity.firestoreId}, roomId=${unsyncedEntity.id} [$collectionName]")
                }

                val updatedEntities = commitBatchAndUpdate(
                    batch = batch,
                    batchSize = currFirestoreDocBatch.size,
                    collectionName = collectionName,
                    currFirestoreDocBatch = currFirestoreDocBatch,
                    onUpdateMany = onUpdateMany,
                )

                // Return updated entities from this thread
                updatedEntities
            }
        }

        return syncedEntities
    }

    private suspend fun commitBatchAndUpdate(
        batch: WriteBatch,
        batchSize: Int,
        collectionName: String,
        currFirestoreDocBatch: MutableList<DocumentReference>,
        onUpdateMany: suspend (List<BaseFirestoreDoc>) -> Unit
    ): List<BaseFirestoreDoc> {
        batch.commit().await()
        Log.d(TAG, "Uploaded $batchSize entities [$collectionName]")

        val dataSource = dataSources[collectionName]
        // Get updated entities from Firestore and update local database so lastUpdated is up to date
        val updatedEntities = currFirestoreDocBatch.flatMapParallel(10) { docChunk ->
            docChunk.map { docRef ->
                runCatching {
                    dataSource?.let { docRef.get().await().toObject(it.firestoreDocClass.java) }
                }.onFailure { e ->
                    Log.e(
                        TAG,
                        "Failed to fetch doc ${docRef.id}: ${e.message} [$collectionName]",
                        e
                    )
                    FirebaseCrashlytics.getInstance().recordException(e)
                }.getOrNull()
            }
        }.filterNotNull()

        onUpdateMany(updatedEntities)

        Log.d(TAG, "Batch updated ${updatedEntities.size} entities locally [$collectionName]")
        FirebaseCrashlytics.getInstance().log("Batch updated ${updatedEntities.size} entities locally [$collectionName]")

        return updatedEntities
    }

    private suspend fun getMaxLastUpdatedTimestamp(collection: CollectionReference): Date? {
        return suspendCoroutine { cont ->
            collection
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val maxTimestamp = snapshot.documents.firstOrNull()?.getDate("lastUpdated")
                    cont.resume(maxTimestamp)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }
}