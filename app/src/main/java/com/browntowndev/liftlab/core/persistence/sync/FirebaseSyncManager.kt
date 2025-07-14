package com.browntowndev.liftlab.core.persistence.sync

import android.util.Log
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.dao.*
import com.browntowndev.liftlab.core.persistence.dtos.firebase.BaseFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.SyncMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.collections.flatten


class FirebaseSyncManager (
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val liftLabDatabase: LiftLabDatabase,
) {
    private val programsDao: ProgramsDao get() = liftLabDatabase.programsDao()
    private val workoutsDao: WorkoutsDao get() = liftLabDatabase.workoutsDao()
    private val workoutLiftsDao: WorkoutLiftsDao get() = liftLabDatabase.workoutLiftsDao()
    private val customSetsDao: CustomSetsDao get() = liftLabDatabase.customSetsDao()
    private val liftsDao: LiftsDao get() = liftLabDatabase.liftsDao()
    private val workoutLogEntriesDao: WorkoutLogEntryDao get() = liftLabDatabase.workoutLogEntryDao()
    private val setLogEntryDao: SetLogEntryDao get() = liftLabDatabase.setLogEntryDao()
    private val volumeMetricChartDao: VolumeMetricChartsDao get() = liftLabDatabase.volumeMetricChartsDao()
    private val liftMetricChartDao: LiftMetricChartsDao get() = liftLabDatabase.liftMetricChartsDao()
    private val restTimerInProgressDao: RestTimerInProgressDao get() = liftLabDatabase.restTimerInProgressDao()
    private val workoutInProgressDao: WorkoutInProgressDao get() = liftLabDatabase.workoutInProgressDao()
    private val previousSetResultsDao: PreviousSetResultDao get() = liftLabDatabase.previousSetResultsDao()
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao get() = liftLabDatabase.historicalWorkoutNamesDao()
    private val syncDao: SyncDao get() = liftLabDatabase.syncDao()

    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val BATCH_SIZE = 400 // Stay under 500 to be safe
    }

    suspend fun syncAll() = coroutineScope {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return@coroutineScope
        }

        val userId = currentUser.uid
        Log.d(TAG, "User $userId logged in. Starting initial sync.")

        try {
            syncEntities(
                userId = userId,
                collectionName ="lifts",
                localEntities = liftsDao.getAll().map { it.toFirebaseDto() },
                onUpdateMany = { syncDtos ->
                    liftsDao.updateMany(syncDtos.map { it.toEntity() })
                }
            )

            awaitAll(
                async {
                    syncEntities(
                        userId = userId,
                        collectionName = "programs",
                        localEntities = programsDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            programsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionName = "workouts",
                        localEntities = workoutsDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            workoutsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionName = "workoutLifts",
                        localEntities = workoutLiftsDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            workoutLiftsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionName = "customSets",
                        localEntities = customSetsDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            customSetsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionName = "previousSetResults",
                        localEntities = previousSetResultsDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            previousSetResultsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionName = "historicalWorkoutNames",
                        localEntities = historicalWorkoutNamesDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            historicalWorkoutNamesDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionName = "workoutLogEntries",
                        localEntities = workoutLogEntriesDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            workoutLogEntriesDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionName = "setLogEntries",
                        localEntities = setLogEntryDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            setLogEntryDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionName = "volumeMetricCharts",
                        localEntities = volumeMetricChartDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            volumeMetricChartDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionName = "liftMetricCharts",
                        localEntities = liftMetricChartDao.getAll().map { it.toFirebaseDto() },
                        onUpdateMany = { syncDtos ->
                            liftMetricChartDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionName = "restTimerInProgress",
                        localEntities = restTimerInProgressDao.get()?.let { listOf(it.toFirebaseDto()) } ?: emptyList(),
                        onUpdateMany = { syncDtos ->
                            restTimerInProgressDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionName = "workoutInProgress",
                        localEntities = workoutInProgressDao.get()?.let { listOf(it.toFirebaseDto()) } ?: emptyList(),
                        onUpdateMany = { syncDtos ->
                            workoutInProgressDao.updateMany(syncDtos.map { it.toEntity() })
                        }
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
        userId: String,
        collectionName: String,
        localEntities: List<T>,
        crossinline onUpdateMany: suspend (List<T>) -> Unit
    ) {
        val allSyncedEntities: MutableList<T> = mutableListOf()
        val lastSyncDate = syncDao.getForCollection(collectionName)?.lastSyncTimestamp ?: Date(0)
        val localEntitiesInFirestore = localEntities
            .filter { it.firestoreId != null }
            .associateBy { it.firestoreId!! }
        val collection = firestore
            .collection("users")
            .document(userId)
            .collection(collectionName)

        val updatedEntities = updateOutdatedLocalEntities<T>(
            collection = collection,
            lastSyncDate = lastSyncDate,
            localEntitiesInFirestore = localEntitiesInFirestore,
            onUpdateMany = onUpdateMany,
            collectionName = collectionName,
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
            val syncMetadata = SyncMetadata(collectionName = collectionName, lastSyncTimestamp = latestTimestamp)
            syncDao.upsert(syncMetadata)
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

                        unsyncedEntity.synced = true
                        currFirestoreDocBatch.add(docRef)
                        batch.set(docRef, unsyncedEntity)
                        Log.d(TAG, "Syncing entity ${unsyncedEntity.firestoreId} [$collectionName]")
                    }

                    batch.commit().await()
                    Log.d(TAG, "Synced ${unsyncedEntityChunk.size} entities [$collectionName]")

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

                    // Return updated entities from this thread
                    updatedEntities
                }
            }
        }.awaitAll().flatten()

        return syncedEntities
    }

    private suspend inline fun <reified T : BaseFirebaseDto> updateOutdatedLocalEntities(
        collection: CollectionReference,
        lastSyncDate: Date,
        localEntitiesInFirestore: Map<String, T>,
        onUpdateMany: suspend (List<T>) -> Unit,
        collectionName: String,
    ): List<T> {
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
                        Log.d(TAG, "Found outdated entity: firestore last updated ${firestoreEntity.lastUpdated}, local last updated $localLastUpdated [$collectionName]")
                    }
                }

                if (currFirestoreBatch.isNotEmpty()) {
                    onUpdateMany(currFirestoreBatch)
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