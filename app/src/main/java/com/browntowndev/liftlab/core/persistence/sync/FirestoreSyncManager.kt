package com.browntowndev.liftlab.core.persistence.sync

import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.copyForUpload
import com.browntowndev.liftlab.core.persistence.dtos.firebase.BaseFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.SyncMetadataDto
import com.browntowndev.liftlab.core.persistence.repositories.firebase.CustomLiftSetsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.HistoricalWorkoutNamesSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.LiftMetricChartsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.LiftsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.PreviousSetResultsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.ProgramsSyncRepository
import com.browntowndev.liftlab.core.persistence.repositories.firebase.RestTimerInProgressSyncRepository
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
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.collections.flatten


class FirestoreSyncManager (
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val customSetSyncRepository: CustomLiftSetsSyncRepository,
    private val historicalWorkoutNameSyncRepository: HistoricalWorkoutNamesSyncRepository,
    private val liftMetricChartSyncRepository: LiftMetricChartsSyncRepository,
    private val liftsSyncRepository: LiftsSyncRepository,
    private val previousSetResultSyncRepository: PreviousSetResultsSyncRepository,
    private val programSyncRepository: ProgramsSyncRepository,
    private val restTimerInProgressSyncRepository: RestTimerInProgressSyncRepository,
    private val setLogEntrySyncRepository: SetLogEntriesSyncRepository,
    private val volumeMetricChartSyncRepository: VolumeMetricChartsSyncRepository,
    private val workoutInProgressSyncRepository: WorkoutInProgressSyncRepository,
    private val workoutLiftSyncRepository: WorkoutLiftsSyncRepository,
    private val workoutLogEntrySyncRepository: WorkoutLogEntriesSyncRepository,
    private val workoutSyncRepository: WorkoutsSyncRepository,
    private val syncRepository: SyncMetadataRepository,
) {
    private val userId: String? get() = firebaseAuth.currentUser?.uid

    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val BATCH_SIZE = 400 // Stay under 500 to be safe
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

        firestoreIds.chunked(BATCH_SIZE).forEach { chunk ->
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

            try {
                batch.commit().await()
            } catch (e: Exception) {
                Log.e(TAG, "Firestore batch delete failed: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                // TODO: Add retry queue
            }
        }
    }

    internal suspend inline fun <reified T : BaseFirebaseDto> syncSingle(
        collectionName: String,
        entity: T,
        noinline onSynced: suspend (firestoreEntity: T) -> Unit
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        val collection = firestore.collection("users")
            .document(userId!!)
            .collection(collectionName)

        val docRef = if (entity.firestoreId != null)
            collection.document(entity.firestoreId!!)
        else
            collection.document()

        val toUpload = entity.copyForUpload(docRef.id)

        try {
            docRef.set(toUpload).await()
            val snapshot = docRef.get().await()
            val firestoreEntity = snapshot.toObject<T>()
            if (firestoreEntity != null) {
                onSynced(firestoreEntity)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync failed: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }


    internal suspend inline fun <reified T : BaseFirebaseDto> syncMany(
        collectionName: String,
        entities: List<T>,
        crossinline onSynced: suspend (List<T>) -> Unit
    ) {
        if (userId == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        entities.chunked(BATCH_SIZE).forEach { entityChunk ->
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

            coroutineScope {
                try {
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
                localEntities = liftsSyncRepository.getAll(),
                onUpdateMany = liftsSyncRepository::updateMany,
                onUpsertMany = liftsSyncRepository::upsertMany,
            )

            awaitAll(
                async {
                    syncEntities(
                        collection = programSyncRepository.collection,
                        lastSyncDate = syncRepository.get(programSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = programSyncRepository.getAll(),
                        onUpdateMany = programSyncRepository::updateMany,
                        onUpsertMany = programSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = workoutSyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = workoutSyncRepository.getAll(),
                        onUpdateMany = workoutSyncRepository::updateMany,
                        onUpsertMany = workoutSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = workoutLiftSyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutLiftSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = workoutLiftSyncRepository.getAll(),
                        onUpdateMany = workoutLiftSyncRepository::updateMany,
                        onUpsertMany = workoutLiftSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = customSetSyncRepository.collection,
                        lastSyncDate = syncRepository.get(customSetSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = customSetSyncRepository.getAll(),
                        onUpdateMany = customSetSyncRepository::updateMany,
                        onUpsertMany = customSetSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = previousSetResultSyncRepository.collection,
                        lastSyncDate = syncRepository.get(previousSetResultSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = previousSetResultSyncRepository.getAll(),
                        onUpdateMany = previousSetResultSyncRepository::updateMany,
                        onUpsertMany = previousSetResultSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = historicalWorkoutNameSyncRepository.collection,
                        lastSyncDate = syncRepository.get(historicalWorkoutNameSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = historicalWorkoutNameSyncRepository.getAll(),                        onUpdateMany = historicalWorkoutNameSyncRepository::updateMany,
                        onUpsertMany = historicalWorkoutNameSyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = workoutLogEntrySyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutLogEntrySyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = workoutLogEntrySyncRepository.getAll(),
                        onUpdateMany = { syncDtos ->
                            workoutLogEntrySyncRepository.updateMany(syncDtos)
                        },                        onUpsertMany = workoutLogEntrySyncRepository::upsertMany,
                    )
                    syncEntities(
                        collection = setLogEntrySyncRepository.collection,
                        lastSyncDate = syncRepository.get(setLogEntrySyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = setLogEntrySyncRepository.getAll(),
                        onUpdateMany = setLogEntrySyncRepository::updateMany,
                        onUpsertMany = setLogEntrySyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = volumeMetricChartSyncRepository.collection,
                        lastSyncDate = syncRepository.get(volumeMetricChartSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = volumeMetricChartSyncRepository.getAll(),                        onUpdateMany = volumeMetricChartSyncRepository::updateMany,
                        onUpsertMany = volumeMetricChartSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = liftMetricChartSyncRepository.collection,
                        lastSyncDate = syncRepository.get(liftMetricChartSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = liftMetricChartSyncRepository.getAll(),                        onUpdateMany = liftMetricChartSyncRepository::updateMany,
                        onUpsertMany = liftMetricChartSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = restTimerInProgressSyncRepository.collection,
                        lastSyncDate = syncRepository.get(restTimerInProgressSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = restTimerInProgressSyncRepository.getAll(),                        onUpdateMany = restTimerInProgressSyncRepository::updateMany,
                        onUpsertMany = restTimerInProgressSyncRepository::upsertMany,
                    )
                },
                async {
                    syncEntities(
                        collection = workoutInProgressSyncRepository.collection,
                        lastSyncDate = syncRepository.get(workoutInProgressSyncRepository.collectionName)?.lastSyncTimestamp ?: Date(0),
                        localEntities = workoutInProgressSyncRepository.getAll(),
                        onUpdateMany = workoutInProgressSyncRepository::updateMany,
                        onUpsertMany = workoutInProgressSyncRepository::upsertMany,
                    )
                },
            )

            Log.d(TAG, "Sync completed for user $userId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync for user $userId: ${e.message}", e)

            FirebaseCrashlytics.getInstance().log("Sync failed for user $userId")
            FirebaseCrashlytics.getInstance().recordException(e)

            throw SyncFailedException()
        }
    }

    private suspend inline fun<reified T: BaseFirebaseDto> syncEntities(
        collection: CollectionReference,
        lastSyncDate: Date,
        localEntities: List<T>,
        crossinline onUpdateMany: suspend (List<T>) -> Unit,
        crossinline onUpsertMany: suspend (List<T>) -> Unit,
    ) {
        val collectionName = collection.id
        val allSyncedEntities: MutableList<T> = mutableListOf()
        val localEntitiesInFirestore = localEntities
            .filter { it.firestoreId != null }
            .associateBy { it.firestoreId!! }

        val updatedEntities = updateOutdatedLocalEntities<T>(
            collection = collection,
            lastSyncDate = lastSyncDate,
            localEntitiesInFirestore = localEntitiesInFirestore,
            onUpsertMany = onUpsertMany,
        )

        allSyncedEntities += updatedEntities

        // Sync any unsynced entities up to Firestore
        val unsyncedEntities = localEntities.filter { !it.synced }
        if (unsyncedEntities.isNotEmpty()) {
            val syncedEntities = uploadUnsyncedEntities<T>(unsyncedEntities, collection, collectionName, onUpdateMany)
            allSyncedEntities += syncedEntities
        }

        // Find the newest timestamp across all synced entities
        val latestTimestamp = allSyncedEntities
            .mapNotNull { it.lastUpdated }
            .maxOrNull()

        // Update the last sync timestamp
        if (latestTimestamp != null) {
            val syncMetadata = SyncMetadataDto(collectionName = collectionName, lastSyncTimestamp = latestTimestamp)
            syncRepository.upsert(syncMetadata)
        }
    }

    private suspend inline fun <reified T : BaseFirebaseDto> uploadUnsyncedEntities(
        localEntities: List<T>,
        collection: CollectionReference,
        collectionName: String,
        crossinline onUpdateMany: suspend (List<T>) -> Unit
    ): List<T> {
        val unsyncedEntities = localEntities.filter { !it.synced }
        if (unsyncedEntities.isEmpty()) return emptyList()

        Log.d(TAG, "Uploading ${unsyncedEntities.size} unsynced entities [$collectionName]")

        val syncedEntities = coroutineScope {
            unsyncedEntities.chunked(BATCH_SIZE).map { unsyncedEntityChunk ->
                async {
                    val currFirestoreDocBatch = mutableListOf<DocumentReference>()
                    val batch = firestore.batch()

                    unsyncedEntityChunk.forEach { unsyncedEntity ->
                        val docRef = unsyncedEntity.firestoreId?.let(collection::document)
                            ?: collection.document().also { unsyncedEntity.firestoreId = it.id }

                        unsyncedEntity.copyForUpload(docRef.id)
                        batch.set(docRef, unsyncedEntity)

                        currFirestoreDocBatch.add(docRef)
                        Log.d(TAG, "Syncing entity ${unsyncedEntity.firestoreId} [$collectionName]")
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
        }.awaitAll().flatten()

        return syncedEntities
    }

    private suspend inline fun <reified T : BaseFirebaseDto> CoroutineScope.commitBatchAndUpdate(
        batch: WriteBatch,
        batchSize: Int,
        collectionName: String,
        currFirestoreDocBatch: MutableList<DocumentReference>,
        onUpdateMany: suspend (List<T>) -> Unit
    ): List<T> {
        batch.commit().await()
        Log.d(TAG, "Synced $batchSize entities [$collectionName]")

        // Get updated entities from Firestore and update local database so lastUpdated is up to date
        val updatedEntities = currFirestoreDocBatch.map { docRef ->
            async {
                runCatching {
                    docRef.get().await().toObject<T>()
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()

        onUpdateMany(updatedEntities)

        Log.d(
            TAG,
            "Batch updated ${updatedEntities.size} entities locally [$collectionName]"
        )
        return updatedEntities
    }

    private suspend inline fun <reified T : BaseFirebaseDto> updateOutdatedLocalEntities(
        collection: CollectionReference,
        lastSyncDate: Date,
        localEntitiesInFirestore: Map<String, T>,
        onUpsertMany: suspend (List<T>) -> Unit,
    ): List<T> {
        val collectionName = collection.id
        val allSyncedEntities = mutableListOf<T>()
        var lastVisible: DocumentSnapshot? = null
        var done = false

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
                val currFirestoreBatch = mutableListOf<T>()
                for (doc in docs) {
                    val firestoreEntity = doc.toObject<T>() ?: continue
                    val localEntity = localEntitiesInFirestore[firestoreEntity.firestoreId]
                    val localLastUpdated = localEntity?.lastUpdated ?: Date(0)

                    if (firestoreEntity.lastUpdated?.after(localLastUpdated) == true) {
                        currFirestoreBatch.add(firestoreEntity)
                        Log.d(TAG, "Found outdated entity ${firestoreEntity.firestoreId}: firestore last updated ${firestoreEntity.lastUpdated}, local last updated $localLastUpdated [$collectionName]")
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
}