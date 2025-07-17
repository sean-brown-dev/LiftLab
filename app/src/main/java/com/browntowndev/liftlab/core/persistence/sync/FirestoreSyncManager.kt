package com.browntowndev.liftlab.core.persistence.sync

import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.copyForUpload
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.common.flatMapParallel
import com.browntowndev.liftlab.core.common.forEachParallel
import com.browntowndev.liftlab.core.persistence.dtos.firestore.BaseFirestoreDto
import com.browntowndev.liftlab.core.persistence.dtos.firestore.SyncMetadataDto
import com.browntowndev.liftlab.core.persistence.repositories.firebase.CustomLiftSetsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.HistoricalWorkoutNamesSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.LiftMetricChartsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.LiftsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.PreviousSetResultsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.ProgramsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.SetLogEntriesSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.SyncMetadataRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.VolumeMetricChartsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.WorkoutInProgressSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.WorkoutLiftsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.WorkoutLogEntriesSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.WorkoutsSyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class FirestoreSyncManager (
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val syncScope: CoroutineScope,
    private val customLiftSetsSyncRepository: CustomLiftSetsSyncRepository,
    private val historicalWorkoutNamesSyncRepository: HistoricalWorkoutNamesSyncRepository,
    private val liftMetricChartsSyncRepository: LiftMetricChartsSyncRepository,
    private val liftsSyncRepository: LiftsSyncRepository,
    private val previousSetResultsSyncRepository: PreviousSetResultsSyncRepository,
    private val programsSyncRepository: ProgramsSyncRepository,
    private val setLogEntriesSyncRepository: SetLogEntriesSyncRepository,
    private val volumeMetricChartsSyncRepository: VolumeMetricChartsSyncRepository,
    private val workoutInProgressSyncRepository: WorkoutInProgressSyncRepository,
    private val workoutLiftsSyncRepository: WorkoutLiftsSyncRepository,
    private val workoutLogEntriesSyncRepository: WorkoutLogEntriesSyncRepository,
    private val workoutsSyncRepository: WorkoutsSyncRepository,
    private val syncRepository: SyncMetadataRepository,
) {
    private val userId: String? get() = firebaseAuth.currentUser?.uid
    private val deletionWatcherJobs: MutableMap<String, Job> = ConcurrentHashMap()

    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val BATCH_SIZE = 400 // Stay under 500 to be safe

        // Queue every distinct request
        private val syncQueue: MutableList<SyncQueueEntry> = mutableListOf()

        // One running sync per collection
        private val processingSyncs: MutableMap<String, SyncQueueEntry> = mutableMapOf()

        private val mutex: Mutex = Mutex()
    }

    fun enqueueSyncRequest(entry: SyncQueueEntry) = syncScope.fireAndForgetSync {
        val shouldStart = mutex.withLock {
            val isAlreadyProcessing = processingSyncs.containsKey(entry.collectionName)
            if (!isAlreadyProcessing) {
                processingSyncs.put(entry.collectionName, entry)
            } else {
                syncQueue.add(entry)
                val uniqueEntries = syncQueue.distinct()
                syncQueue.clear()
                syncQueue.addAll(uniqueEntries)
            }

            !isAlreadyProcessing
        }

        if (shouldStart) {
            processQueue(entry)
        }
    }

    private suspend fun processQueue(queueEntry: SyncQueueEntry) {
        var entryToProcess = queueEntry.copy()

        while (true) {when (entryToProcess.collectionName) {
            customLiftSetsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = customLiftSetsSyncRepository.collectionName,
                    entities = customLiftSetsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = customLiftSetsSyncRepository::upsertMany,
                )

            historicalWorkoutNamesSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = historicalWorkoutNamesSyncRepository.collectionName,
                    entities = historicalWorkoutNamesSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = historicalWorkoutNamesSyncRepository::upsertMany,
                )

            liftMetricChartsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = liftMetricChartsSyncRepository.collectionName,
                    entities = liftMetricChartsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = liftMetricChartsSyncRepository::upsertMany,
                )

            liftsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = liftsSyncRepository.collectionName,
                    entities = liftsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = liftsSyncRepository::upsertMany,
                )

            previousSetResultsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = previousSetResultsSyncRepository.collectionName,
                    entities = previousSetResultsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = previousSetResultsSyncRepository::upsertMany,
                )

            programsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = programsSyncRepository.collectionName,
                    entities = programsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = programsSyncRepository::upsertMany,
                )

            setLogEntriesSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = setLogEntriesSyncRepository.collectionName,
                    entities = setLogEntriesSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = setLogEntriesSyncRepository::upsertMany,
                )

            volumeMetricChartsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = volumeMetricChartsSyncRepository.collectionName,
                    entities = volumeMetricChartsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = volumeMetricChartsSyncRepository::upsertMany,
                )

            workoutInProgressSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = workoutInProgressSyncRepository.collectionName,
                    entities = workoutInProgressSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = workoutInProgressSyncRepository::upsertMany,
                )

            workoutLiftsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = workoutLiftsSyncRepository.collectionName,
                    entities = workoutLiftsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = workoutLiftsSyncRepository::upsertMany,
                )

            workoutLogEntriesSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = workoutLogEntriesSyncRepository.collectionName,
                    entities = workoutLogEntriesSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = workoutLogEntriesSyncRepository::upsertMany,
                )

            workoutsSyncRepository.collectionName ->
                executeSyncRequest(
                    collectionName = workoutsSyncRepository.collectionName,
                    entities = workoutsSyncRepository.getMany(entryToProcess.roomEntityIds),
                    syncType = entryToProcess.syncType,
                    onSynced = workoutsSyncRepository::upsertMany,
                )

            else -> {}
        }

            val nextEntry = mutex.withLock {
                val nextEntryForCollection = syncQueue.firstOrNull { it.collectionName == entryToProcess.collectionName }
                syncQueue.remove(nextEntryForCollection)

                val hasMoreWork = nextEntryForCollection != null
                if (!hasMoreWork)
                    processingSyncs.remove(entryToProcess.collectionName)

                nextEntryForCollection
            }

            if (nextEntry != null)
                entryToProcess = nextEntry
            else break
        }
    }

    private suspend inline fun<reified T: BaseFirestoreDto> executeSyncRequest(
        collectionName: String,
        entities: List<T>,
        syncType: SyncType,
        noinline onSynced: suspend (List<T>) -> Unit,
    ) {
        try {
            if (entities.size == 1 && syncType == SyncType.Sync) {
                syncSingle(
                    collectionName = collectionName,
                    entity = entities[0],
                    onSynced = {
                        onSynced(listOf(it))
                    }
                )
            } else if (entities.size > 1 && syncType == SyncType.Sync) {
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

    fun tryStartDeletionWatchers() {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping batch delete.")
            return
        }

        tryStartDeletionWatcher(
            collectionName = workoutsSyncRepository.collectionName,
            entityFlow = workoutsSyncRepository.getAllFlow()
        )
        tryStartDeletionWatcher(
            collectionName = workoutLiftsSyncRepository.collectionName,
            entityFlow = workoutLiftsSyncRepository.getAllFlow()
        )
        tryStartDeletionWatcher(
            collectionName = customLiftSetsSyncRepository.collectionName,
            entityFlow = customLiftSetsSyncRepository.getAllFlow()
        )
        tryStartDeletionWatcher(
            collectionName = workoutInProgressSyncRepository.collectionName,
            entityFlow = workoutInProgressSyncRepository.getAllFlow()
        )
    }

    private fun<T: BaseFirestoreDto> tryStartDeletionWatcher(
        collectionName: String,
        entityFlow: Flow<List<T>>,
    ) {
        val existingJob = deletionWatcherJobs[collectionName]
        if (existingJob?.isActive == true) {
            Log.d(TAG, "Deletion watcher for $collectionName already active.")
            return
        }

        val job = syncScope.fireAndForgetSync {
            val knownIds = ConcurrentHashMap.newKeySet<Long>()
            entityFlow.collect { currentDtos ->
                try {
                    val currentIds = currentDtos.map { it.id }.toSet()
                    val deletedIds = knownIds - currentIds

                    if (deletedIds.isNotEmpty()) {
                        deleteMany(collectionName, deletedIds.map { it.toString() })
                    }

                    knownIds.clear()
                    knownIds.addAll(currentIds)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during deletion watcher for $collectionName: ${e.message}", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        deletionWatcherJobs[collectionName] = job
    }

    suspend fun deleteSingle(
        collectionName: String,
        firestoreId: String,
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        try {
            val docRef = firestore.collection("users")
                .document(userId!!)
                .collection(collectionName)
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

    suspend fun deleteMany(
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
                val batch = firestore.batch()
                val deletedIds = mutableListOf<String>()

                chunk.forEach { firestoreId ->
                    val docRef = firestore.collection("users")
                        .document(userId!!)
                        .collection(collectionName)
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

    internal suspend inline fun <reified T : BaseFirestoreDto> syncSingle(
        collectionName: String,
        entity: T,
        noinline onSynced: suspend (firestoreEntity: T) -> Unit
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        try {
            val collection = firestore.collection("users")
                .document(userId!!)
                .collection(collectionName)

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

    internal suspend inline fun <reified T : BaseFirestoreDto> syncMany(
        collectionName: String,
        entities: List<T>,
        crossinline onSynced: suspend (List<T>) -> Unit
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        coroutineScope {
            entities.forEachParallel(10) { entityChunk ->
                try {
                    val batch = firestore.batch()
                    val firestoreDocumentBatch = mutableListOf<DocumentReference>()
                    entityChunk.fastForEach { entity ->
                        val collection = firestore.collection("users")
                            .document(userId!!)
                            .collection(collectionName)

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

        try {
            syncEntities(
                collection = liftsSyncRepository.collection,
                lastSyncDate = syncRepository.get(liftsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                onGetLocalEntities = liftsSyncRepository::getAll,
                onUpdateMany = liftsSyncRepository::updateMany,
                onUpsertMany = liftsSyncRepository::upsertMany,
            )

            awaitAll(
                async {
                    syncEntities(
                        collection = programsSyncRepository.collection,
                        lastSyncDate = syncRepository.get(programsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = programsSyncRepository::getAll,
                        onUpdateMany = programsSyncRepository::updateMany,
                        onUpsertMany = programsSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = workoutsSyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = workoutsSyncRepository::getAll,
                        onUpdateMany = workoutsSyncRepository::updateMany,
                        onUpsertMany = workoutsSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = workoutLiftsSyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutLiftsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = workoutLiftsSyncRepository::getAll,
                        onUpdateMany = workoutLiftsSyncRepository::updateMany,
                        onUpsertMany = workoutLiftsSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = customLiftSetsSyncRepository.collection,
                        lastSyncDate = syncRepository.get(customLiftSetsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = customLiftSetsSyncRepository::getAll,
                        onUpdateMany = customLiftSetsSyncRepository::updateMany,
                        onUpsertMany = customLiftSetsSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = previousSetResultsSyncRepository.collection,
                        lastSyncDate = syncRepository.get(previousSetResultsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = previousSetResultsSyncRepository::getAll,
                        onUpdateMany = previousSetResultsSyncRepository::updateMany,
                        onUpsertMany = previousSetResultsSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = historicalWorkoutNamesSyncRepository.collection,
                        lastSyncDate = syncRepository.get(historicalWorkoutNamesSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = historicalWorkoutNamesSyncRepository::getAll,
                        onUpdateMany = historicalWorkoutNamesSyncRepository::updateMany,
                        onUpsertMany = historicalWorkoutNamesSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = workoutLogEntriesSyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutLogEntriesSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = workoutLogEntriesSyncRepository::getAll,
                        onUpdateMany = workoutLogEntriesSyncRepository::updateMany,
                        onUpsertMany = workoutLogEntriesSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = setLogEntriesSyncRepository.collection,
                        lastSyncDate = syncRepository.get(setLogEntriesSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = setLogEntriesSyncRepository::getAll,
                        onUpdateMany = setLogEntriesSyncRepository::updateMany,
                        onUpsertMany = setLogEntriesSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = volumeMetricChartsSyncRepository.collection,
                        lastSyncDate = syncRepository.get(volumeMetricChartsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = volumeMetricChartsSyncRepository::getAll,
                        onUpdateMany = volumeMetricChartsSyncRepository::updateMany,
                        onUpsertMany = volumeMetricChartsSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = liftMetricChartsSyncRepository.collection,
                        lastSyncDate = syncRepository.get(liftMetricChartsSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = liftMetricChartsSyncRepository::getAll,
                        onUpdateMany = liftMetricChartsSyncRepository::updateMany,
                        onUpsertMany = liftMetricChartsSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = workoutInProgressSyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutInProgressSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        onGetLocalEntities = workoutInProgressSyncRepository::getAll,
                        onUpdateMany = workoutInProgressSyncRepository::updateMany,
                        onUpsertMany = workoutInProgressSyncRepository::upsertMany,
                    )
                },
            )

            Log.d(TAG, "Sync completed for user $userId.")
            FirebaseCrashlytics.getInstance().log("Sync completed for user $userId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync for user $userId: ${e.message}", e)

            FirebaseCrashlytics.getInstance().log("Sync failed for user $userId")
            FirebaseCrashlytics.getInstance().recordException(e)

            throw SyncFailedException()
        }
    }

    private suspend inline fun<reified T: BaseFirestoreDto> syncEntities(
        collection: CollectionReference,
        lastSyncDate: Date,
        onGetLocalEntities: suspend () -> List<T>,
        crossinline onUpdateMany: suspend (List<T>) -> Unit,
        crossinline onUpsertMany: suspend (List<T>) -> Unit,
    ) {
        val collectionName = collection.id
        val allSyncedEntities: MutableList<T> = mutableListOf()

        // Update local entities that are outdated first since cloud wins.
        // Prevents duplicate ids from being uploaded to Firestore since any locals
        // with different/no firestoreId will be updated here.
        allSyncedEntities += updateOutdatedLocalEntities<T>(
            collection = collection,
            lastSyncDate = lastSyncDate,
            localEntities = onGetLocalEntities(),
            onUpsertMany = onUpsertMany,
        )

        // Sync any unsynced entities up to Firestore
        allSyncedEntities += uploadUnsyncedEntities<T>(
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
            val syncMetadata = SyncMetadataDto(collectionName = collectionName, lastSyncTimestamp = latestTimestamp)
            syncRepository.upsert(syncMetadata)
        } else {
            val maxLastUpdated = getMaxLastUpdatedTimestamp(collection) ?: return
            val syncMetadata = SyncMetadataDto(collectionName = collectionName, lastSyncTimestamp = maxLastUpdated)
            syncRepository.upsert(syncMetadata)
        }
    }

    private suspend inline fun <reified T : BaseFirestoreDto> updateOutdatedLocalEntities(
        collection: CollectionReference,
        lastSyncDate: Date,
        localEntities: List<T>,
        onUpsertMany: suspend (List<T>) -> Unit,
    ): List<T> {
        val collectionName = collection.id
        val allSyncedEntities = mutableListOf<T>()
        var lastVisible: DocumentSnapshot? = null
        var done = false
        val localEntitiesInFirestore = localEntities
            .filter { it.firestoreId != null }
            .associateBy { it.firestoreId!! }

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
                        val firestoreEntity = doc.toObject<T>() ?: return@mapNotNull null
                        val localEntity = localEntitiesInFirestore[firestoreEntity.firestoreId]
                        val localLastUpdated = localEntity?.lastUpdated ?: Date(0)
                        if (firestoreEntity.lastUpdated?.after(localLastUpdated) == true) {
                            Log.d(TAG, "Found outdated entity: ${firestoreEntity.firestoreId}: firestore last updated ${firestoreEntity.lastUpdated}, local last updated $localLastUpdated, local firestoreId ${localEntity?.firestoreId} [$collectionName]")
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

    private suspend inline fun <reified T : BaseFirestoreDto> uploadUnsyncedEntities(
        localEntities: List<T>,
        collection: CollectionReference,
        collectionName: String,
        crossinline onUpdateMany: suspend (List<T>) -> Unit
    ): List<T> {
        val unsyncedEntities = localEntities.filter { !it.synced }
        if (unsyncedEntities.isEmpty()) return emptyList()

        Log.d(TAG, "Uploading ${unsyncedEntities.size} unsynced entities [$collectionName]")
        FirebaseCrashlytics.getInstance().log("Uploading ${unsyncedEntities.size} unsynced entities [$collectionName]")

        val syncedEntities = coroutineScope {
            unsyncedEntities.flatMapParallel(10) { unsyncedEntityChunk ->
                val currFirestoreDocBatch = mutableListOf<DocumentReference>()
                val batch = firestore.batch()

                unsyncedEntityChunk.forEach { unsyncedEntity ->
                    val docRef = unsyncedEntity.firestoreId?.let(collection::document)
                        ?: collection.document()

                    unsyncedEntity.copyForUpload(docRef.id)
                    batch.set(docRef, unsyncedEntity)

                    currFirestoreDocBatch.add(docRef)
                    Log.d(TAG, "Uploading entity ${unsyncedEntity.firestoreId} [$collectionName]")
                }

                val updatedEntities = commitBatchAndUpdate<T>(
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

    private suspend inline fun <reified T : BaseFirestoreDto> CoroutineScope.commitBatchAndUpdate(
        batch: WriteBatch,
        batchSize: Int,
        collectionName: String,
        currFirestoreDocBatch: MutableList<DocumentReference>,
        onUpdateMany: suspend (List<T>) -> Unit
    ): List<T> {
        batch.commit().await()
        Log.d(TAG, "Uploaded $batchSize entities [$collectionName]")

        // Get updated entities from Firestore and update local database so lastUpdated is up to date
        val updatedEntities = currFirestoreDocBatch.flatMapParallel(10) { docChunk ->
            docChunk.map { docRef ->
                runCatching {
                    docRef.get().await().toObject<T>()
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