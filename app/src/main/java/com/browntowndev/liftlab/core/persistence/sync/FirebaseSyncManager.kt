package com.browntowndev.liftlab.core.persistence.sync

import android.util.Log
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.dao.*
import com.browntowndev.liftlab.core.persistence.dtos.firebase.BaseFirebaseDto
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Date

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
                collectionPath ="lifts",
                localEntities = liftsDao.getAll().map { it.toFirebaseDto() },
                onUpdate = { liftsDao.update(it.toEntity()) },
                onUpdateMany = { syncDtos ->
                    liftsDao.updateMany(syncDtos.map { it.toEntity() })
                }
            )

            awaitAll(
                async {
                    syncEntities(
                        userId = userId,
                        collectionPath = "programs",
                        localEntities = programsDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { programsDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            programsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionPath = "workouts",
                        localEntities = workoutsDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { workoutsDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            workoutsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionPath = "workoutLifts",
                        localEntities = workoutLiftsDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { workoutLiftsDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            workoutLiftsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionPath = "customSets",
                        localEntities = customSetsDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { customSetsDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            customSetsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionPath = "previousSetResults",
                        localEntities = previousSetResultsDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { previousSetResultsDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            previousSetResultsDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionPath = "historicalWorkoutNames",
                        localEntities = historicalWorkoutNamesDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { historicalWorkoutNamesDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            historicalWorkoutNamesDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionPath = "workoutLogEntries",
                        localEntities = workoutLogEntriesDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { workoutLogEntriesDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            workoutLogEntriesDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                    syncEntities(
                        userId = userId,
                        collectionPath = "setLogEntries",
                        localEntities = setLogEntryDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { setLogEntryDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            setLogEntryDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionPath = "volumeMetricCharts",
                        localEntities = volumeMetricChartDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { volumeMetricChartDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            volumeMetricChartDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionPath = "liftMetricCharts",
                        localEntities = liftMetricChartDao.getAll().map { it.toFirebaseDto() },
                        onUpdate = { liftMetricChartDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            liftMetricChartDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionPath = "restTimerInProgress",
                        localEntities = restTimerInProgressDao.get()?.let { listOf(it.toFirebaseDto()) } ?: emptyList(),
                        onUpdate = { restTimerInProgressDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            restTimerInProgressDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
                async {
                    syncEntities(
                        userId = userId,
                        collectionPath = "workoutInProgress",
                        localEntities = workoutInProgressDao.get()?.let { listOf(it.toFirebaseDto()) } ?: emptyList(),
                        onUpdate = { workoutInProgressDao.update(it.toEntity()) },
                        onUpdateMany = { syncDtos ->
                            workoutInProgressDao.updateMany(syncDtos.map { it.toEntity() })
                        }
                    )
                },
            )

            Log.d(TAG, "Sync completed for user $userId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync for user $userId: ${e.message}", e)
            // TODO: Notify user of sync failure
        }
    }

    private suspend inline fun<reified T: BaseFirebaseDto> syncEntities(
        userId: String,
        collectionPath: String,
        localEntities: List<T>,
        crossinline onUpdate: suspend (T) -> Unit,
        crossinline onUpdateMany: suspend (List<T>) -> Unit
    ) {
        val firestoreWorkoutsCollection =
            firestore.collection("users")
                .document(userId)
                .collection(collectionPath)

        val firestoreSnapshots = firestoreWorkoutsCollection.get().await()
        val localEntitiesInFirestore = localEntities
            .filter { it.firestoreId != null }
            .associateBy { it.firestoreId!! }

        // Update out of date workouts
        firestoreSnapshots.forEach { firestoreSnapshot ->
            val firestoreEntity = firestoreSnapshot.toObject<T>()
            val localEntity = localEntitiesInFirestore[firestoreEntity.firestoreId]
            val localEntityLastModified = localEntity?.lastUpdated ?: Date(0)

            if (localEntity == null || localEntityLastModified < firestoreEntity.lastUpdated) {
                onUpdate(firestoreEntity)
                Log.d(TAG, "Updated entity ${firestoreEntity.firestoreId}. " +
                        "Local lastupdated: ${localEntity?.lastUpdated}, " +
                        "Firestore updated: ${firestoreEntity.lastUpdated}")
            }
        }

        val unsyncedEntities = localEntities.filter { !it.synced }
        if (unsyncedEntities.isNotEmpty()) {
            val collection = firestore.collection("users")
                .document(userId)
                .collection(collectionPath)

            val chunkedEntities = unsyncedEntities.chunked(BATCH_SIZE)
            for (unsyncedEntityChunk in chunkedEntities) {
                val newDocsForBatch = mutableListOf<DocumentReference>()
                val batch = firestore.batch()

                unsyncedEntityChunk.forEach { unsyncedEntity ->
                    val docRef = unsyncedEntity.firestoreId?.let(collection::document)
                        ?: collection.document().also { unsyncedEntity.firestoreId = it.id }

                    unsyncedEntity.synced = true
                    newDocsForBatch.add(docRef)
                    batch.set(docRef, unsyncedEntity)
                    Log.d(TAG, "Syncing entity ${unsyncedEntity.firestoreId}")
                }

                batch.commit().await()
                Log.d(TAG, "Synced ${unsyncedEntityChunk.size} entities")

                // Re-fetch each document to get actual lastUpdated
                coroutineScope {
                    newDocsForBatch.map { docRef ->
                        async {
                            val snapshot = docRef.get().await()
                            snapshot.toObject<T>()?.let {
                                onUpdate(it)
                                Log.d(TAG, "Updated entity ${it.firestoreId}")
                            }
                        }
                    }.awaitAll()
                }
            }
        }
    }
}