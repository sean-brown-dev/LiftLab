package com.browntowndev.liftlab.core.persistence.firestore.sync

import android.util.Log
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.copyForUpload
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.common.flatMapParallel
import com.browntowndev.liftlab.core.common.forEachParallel
import com.browntowndev.liftlab.core.domain.repositories.sync.*
import com.browntowndev.liftlab.core.persistence.firestore.entities.BaseFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.CustomLiftSetFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.HistoricalWorkoutNameFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.LiftFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.LiftMetricChartFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.PreviousSetResultFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.ProgramFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.SetLogEntryFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.SyncMetadataDto
import com.browntowndev.liftlab.core.persistence.firestore.entities.VolumeMetricChartFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutInProgressFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutLiftFirestoreEntity
import com.browntowndev.liftlab.core.persistence.firestore.entities.WorkoutLogEntryFirestoreEntity
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
        when (entryToProcess.collectionName) {
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
    }

    private suspend inline fun<reified T: BaseFirestoreEntity> executeSyncRequest(
        collectionName: String,
        entities: List<T>,
        syncType: SyncType,
        noinline onSynced: suspend (List<T>) -> Unit,
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
                when (collectionName) {
                    customLiftSetsSyncRepository.collectionName -> customLiftSetsSyncRepository.getMany(roomEntityIds)
                    historicalWorkoutNamesSyncRepository.collectionName -> historicalWorkoutNamesSyncRepository.getMany(roomEntityIds)
                    liftMetricChartsSyncRepository.collectionName -> liftMetricChartsSyncRepository.getMany(roomEntityIds)
                    liftsSyncRepository.collectionName -> liftsSyncRepository.getMany(roomEntityIds)
                    previousSetResultsSyncRepository.collectionName -> previousSetResultsSyncRepository.getMany(roomEntityIds)
                    programsSyncRepository.collectionName -> programsSyncRepository.getMany(roomEntityIds)
                    setLogEntriesSyncRepository.collectionName -> setLogEntriesSyncRepository.getMany(roomEntityIds)
                    volumeMetricChartsSyncRepository.collectionName -> volumeMetricChartsSyncRepository.getMany(roomEntityIds)
                    workoutInProgressSyncRepository.collectionName -> workoutInProgressSyncRepository.getMany(roomEntityIds)
                    workoutLiftsSyncRepository.collectionName -> workoutLiftsSyncRepository.getMany(roomEntityIds)
                    workoutLogEntriesSyncRepository.collectionName -> workoutLogEntriesSyncRepository.getMany(roomEntityIds)
                    workoutsSyncRepository.collectionName -> workoutsSyncRepository.getMany(roomEntityIds)
                    else -> listOf()
                }
            },
            onSynced = { collectionName, entities ->
                @Suppress("UNCHECKED_CAST")
                when (collectionName) {
                    customLiftSetsSyncRepository.collectionName -> customLiftSetsSyncRepository.upsertMany(
                        entities as List<CustomLiftSetFirestoreEntity>
                    )
                    historicalWorkoutNamesSyncRepository.collectionName -> historicalWorkoutNamesSyncRepository.upsertMany(
                        entities as List<HistoricalWorkoutNameFirestoreEntity>
                    )
                    liftMetricChartsSyncRepository.collectionName -> liftMetricChartsSyncRepository.upsertMany(
                        entities as List<LiftMetricChartFirestoreEntity>
                    )
                    liftsSyncRepository.collectionName -> liftsSyncRepository.upsertMany(
                        entities as List<LiftFirestoreEntity>
                    )
                    previousSetResultsSyncRepository.collectionName -> previousSetResultsSyncRepository.upsertMany(
                        entities as List<PreviousSetResultFirestoreEntity>
                    )
                    programsSyncRepository.collectionName -> programsSyncRepository.upsertMany(
                        entities as List<ProgramFirestoreEntity>
                    )
                    setLogEntriesSyncRepository.collectionName -> setLogEntriesSyncRepository.upsertMany(
                        entities as List<SetLogEntryFirestoreEntity>
                    )
                    volumeMetricChartsSyncRepository.collectionName -> volumeMetricChartsSyncRepository.upsertMany(
                        entities as List<VolumeMetricChartFirestoreEntity>
                    )
                    workoutInProgressSyncRepository.collectionName -> workoutInProgressSyncRepository.upsertMany(
                        entities as List<WorkoutInProgressFirestoreEntity>
                    )
                    workoutLiftsSyncRepository.collectionName -> workoutLiftsSyncRepository.upsertMany(
                        entities as List<WorkoutLiftFirestoreEntity>
                    )
                    workoutLogEntriesSyncRepository.collectionName -> workoutLogEntriesSyncRepository.upsertMany(
                        entities as List<WorkoutLogEntryFirestoreEntity>
                    )
                    workoutsSyncRepository.collectionName -> workoutsSyncRepository.upsertMany(
                        entities as List<WorkoutFirestoreEntity>
                    )

                    else -> listOf()
                }
            },
            onRequestObjectConversion = { collectionName, documentSnapshot ->
                @Suppress("UNCHECKED_CAST")
                when(collectionName) {
                    customLiftSetsSyncRepository.collectionName ->
                        documentSnapshot.toObject<CustomLiftSetFirestoreEntity>()
                    historicalWorkoutNamesSyncRepository.collectionName ->
                        documentSnapshot.toObject<HistoricalWorkoutNameFirestoreEntity>()
                    liftMetricChartsSyncRepository.collectionName ->
                        documentSnapshot.toObject<LiftMetricChartFirestoreEntity>()
                    liftsSyncRepository.collectionName ->
                        documentSnapshot.toObject<LiftFirestoreEntity>()
                    previousSetResultsSyncRepository.collectionName ->
                        documentSnapshot.toObject<PreviousSetResultFirestoreEntity>()
                    programsSyncRepository.collectionName ->
                        documentSnapshot.toObject<ProgramFirestoreEntity>()
                    setLogEntriesSyncRepository.collectionName ->
                        documentSnapshot.toObject<SetLogEntryFirestoreEntity>()
                    volumeMetricChartsSyncRepository.collectionName ->
                        documentSnapshot.toObject<VolumeMetricChartFirestoreEntity>()
                    workoutInProgressSyncRepository.collectionName ->
                        documentSnapshot.toObject<WorkoutInProgressFirestoreEntity>()
                    workoutLiftsSyncRepository.collectionName ->
                        documentSnapshot.toObject<WorkoutLiftFirestoreEntity>()
                    workoutLogEntriesSyncRepository.collectionName ->
                        documentSnapshot.toObject<WorkoutLogEntryFirestoreEntity>()
                    workoutsSyncRepository.collectionName ->
                        documentSnapshot.toObject<WorkoutFirestoreEntity>()
                    else -> null
                }
            },
        )
    }

    private suspend inline fun<reified T: BaseFirestoreEntity> executeBatchSyncRequest(
        batchSyncCollections: List<BatchSyncCollection>,
        crossinline onRequestEntities: suspend (collectionName: String, roomEntityIds: List<Long>) -> List<T>,
        noinline onSynced: suspend (collectionName: String, entities: List<T>) -> Unit,
        onRequestObjectConversion: (collectionName: String, documentSnapshot: DocumentSnapshot) -> T?,
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

        tryStartDeletionWatcher(
            collectionName = workoutsSyncRepository.collectionName,
            entityFlow = workoutsSyncRepository.getAllFlow(),
            onGetFirestoreIdsForEntitiesWithDeletedParents = { workoutFirestoreDtos ->
                val workoutProgramIds = workoutFirestoreDtos.fastMap { it.programId }.distinct()
                val programIds = programsSyncRepository.getMany(workoutProgramIds)
                    .fastMap { it.id }
                    .toHashSet()

                workoutFirestoreDtos.mapNotNull { dto ->
                    if (dto.programId !in programIds && dto.firestoreId != null) {
                        dto.firestoreId
                    } else null
                }
            },
        )
        tryStartDeletionWatcher(
            collectionName = workoutLiftsSyncRepository.collectionName,
            entityFlow = workoutLiftsSyncRepository.getAllFlow(),
            onGetFirestoreIdsForEntitiesWithDeletedParents = { workoutLiftFirestoreDtos ->
                val workoutLiftWorkoutIds = workoutLiftFirestoreDtos.fastMap { it.workoutId }.distinct()
                val workoutIds = workoutsSyncRepository.getMany(workoutLiftWorkoutIds)
                    .fastMap { it.id }
                    .toHashSet()

                workoutLiftFirestoreDtos.mapNotNull { dto ->
                    if (dto.workoutId !in workoutIds && dto.firestoreId != null) {
                        dto.firestoreId
                    } else null
                }
            },
        )
        tryStartDeletionWatcher(
            collectionName = customLiftSetsSyncRepository.collectionName,
            entityFlow = customLiftSetsSyncRepository.getAllFlow(),
            onGetFirestoreIdsForEntitiesWithDeletedParents = { customLiftSetsFirestoreDtos ->
                val customLiftSetWorkoutIds = customLiftSetsFirestoreDtos.fastMap { it.workoutLiftId }.distinct()
                val workoutLiftIds = workoutLiftsSyncRepository.getMany(customLiftSetWorkoutIds)
                    .fastMap { it.id }
                    .toHashSet()

                customLiftSetsFirestoreDtos.mapNotNull { dto ->
                    if (dto.workoutLiftId !in workoutLiftIds && dto.firestoreId != null) {
                        dto.firestoreId
                    } else null
                }
            },
        )
        tryStartDeletionWatcher(
            collectionName = workoutInProgressSyncRepository.collectionName,
            entityFlow = workoutInProgressSyncRepository.getAllFlow(),
            onGetFirestoreIdsForEntitiesWithDeletedParents = { workoutInProgressFirestoreDtos ->
                val workoutInProgressWorkoutIds = workoutInProgressFirestoreDtos.fastMap { it.workoutId }.distinct()
                val workoutIds = workoutsSyncRepository.getMany(workoutInProgressWorkoutIds)
                    .fastMap { it.id }
                    .toHashSet()

                workoutInProgressFirestoreDtos.mapNotNull { dto ->
                    if (dto.workoutId !in workoutIds && dto.firestoreId != null) {
                        dto.firestoreId
                    } else null
                }
            },
        )
    }

    private fun<T: BaseFirestoreEntity> tryStartDeletionWatcher(
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

    private suspend inline fun <reified T : BaseFirestoreEntity> syncSingle(
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

    private suspend inline fun <reified T : BaseFirestoreEntity> syncMany(
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

    private suspend inline fun<reified T: BaseFirestoreEntity> syncEntities(
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

        // Upsert any unsynced entities up to Firestore
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

    private suspend inline fun <reified T : BaseFirestoreEntity> updateOutdatedLocalEntities(
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
                        val firestoreEntity = doc.toObject<T>() ?: return@mapNotNull null
                        val localEntity = localEntitiesInFirestore[firestoreEntity.firestoreId]
                        val localLastUpdated = localEntity?.lastUpdated ?: Date(0)
                        if (firestoreEntity.lastUpdated?.after(localLastUpdated) == true) {
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

    private suspend inline fun <reified T : BaseFirestoreEntity> uploadUnsyncedEntities(
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
                val batch = firestoreClient.batch()

                unsyncedEntityChunk.forEach { unsyncedEntity ->
                    val docRef = unsyncedEntity.firestoreId?.let(collection::document)
                        ?: collection.document()

                    unsyncedEntity.copyForUpload(docRef.id)
                    batch.set(docRef, unsyncedEntity)

                    currFirestoreDocBatch.add(docRef)
                    Log.d(TAG, "Adding entity to batch. firestoreId=${unsyncedEntity.firestoreId}, roomId=${unsyncedEntity.id} [$collectionName]")
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

    private suspend inline fun <reified T : BaseFirestoreEntity> CoroutineScope.commitBatchAndUpdate(
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