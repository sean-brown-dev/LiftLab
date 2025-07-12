package com.browntowndev.liftlab.core.persistence.sync

import android.util.Log
import com.browntowndev.liftlab.core.persistence.dao.BaseDao
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.entities.BaseEntity
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
import com.browntowndev.liftlab.core.persistence.entities.Workout
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseSyncManager (
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val programsDao: ProgramsDao,
    private val workoutsDao: WorkoutsDao,
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val customSetsDao: CustomSetsDao,
    private val liftsDao: LiftsDao,
    private val workoutLogEntriesDao: WorkoutLogEntryDao,
    private val setLogEntryDao: SetLogEntryDao,
    private val volumeMetricChartDao: VolumeMetricChartsDao,
    private val liftMetricChartDao: LiftMetricChartsDao,
    private val restTimerInProgressDao: RestTimerInProgressDao,
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val previousSetResultsDao: PreviousSetResultDao,
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
) {
    companion object {
        private const val TAG = "FirebaseSyncManager"
    }

    suspend fun syncAll() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No user logged in. Skipping sync.")
            return
        }

        val userId = currentUser.uid
        Log.d(TAG, "User $userId logged in. Starting initial sync.")

        try {
            syncEntities(
                userId = userId,
                collectionPath = "programs",
                localEntities = programsDao.getAll().associateBy { it.id },
                onInsert = { programsDao.insert(it as Program) },
                onRequestId = { (it as Program).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "workouts",
                localEntities = workoutsDao.getAll().associateBy { it.id },
                onInsert = { workoutsDao.insert(it as Workout) },
                onRequestId = { (it as Workout).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "workoutLifts",
                localEntities = workoutLiftsDao.getAll().associateBy { it.id },
                onInsert = { workoutLiftsDao.insert(it as WorkoutLift) },
                onRequestId = { (it as WorkoutLift).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "customSets",
                localEntities = customSetsDao.getAll().associateBy { it.id },
                onInsert = { customSetsDao.insert(it as CustomLiftSet) },
                onRequestId = { (it as CustomLiftSet).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "lifts",
                localEntities = liftsDao.getAll().associateBy { it.id },
                onInsert = { liftsDao.insert(it as Lift) },
                onRequestId = { (it as Lift).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "workoutLogEntries",
                localEntities = workoutLogEntriesDao.getAll().associateBy { it.id },
                onInsert = { workoutLogEntriesDao.insert(it as WorkoutLogEntry) },
                onRequestId = { (it as WorkoutLogEntry).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "setLogEntries",
                localEntities = setLogEntryDao.getAll().associateBy { it.id },
                onInsert = { setLogEntryDao.insert(it as SetLogEntry) },
                onRequestId = { (it as SetLogEntry).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "volumeMetricCharts",
                localEntities = volumeMetricChartDao.getAll().associateBy { it.id },
                onInsert = { volumeMetricChartDao.insert(it as VolumeMetricChart) },
                onRequestId = { (it as VolumeMetricChart).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "liftMetricCharts",
                localEntities = liftMetricChartDao.getAll().associateBy { it.id },
                onInsert = { liftMetricChartDao.insert(it as LiftMetricChart) },
                onRequestId = { (it as LiftMetricChart).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "restTimerInProgress",
                localEntities = restTimerInProgressDao.get()?.let { listOf(it) }?.associateBy { it.id } as? Map<Long, BaseEntity> ?: emptyMap(),
                onInsert = { restTimerInProgressDao.insert(it as RestTimerInProgress) },
                onRequestId = { (it as RestTimerInProgress).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "workoutInProgress",
                localEntities = workoutInProgressDao.get()?.let { listOf(it) }?.associateBy { it.id } as? Map<Long, BaseEntity> ?: emptyMap(),
                onInsert = { workoutInProgressDao.insert(it as WorkoutInProgress) },
                onRequestId = { (it as WorkoutInProgress).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "previousSetResults",
                localEntities = previousSetResultsDao.getAll().associateBy { it.id },
                onInsert = { previousSetResultsDao.insert(it as PreviousSetResult) },
                onRequestId = { (it as PreviousSetResult).id.toString() },
            )
            syncEntities(
                userId = userId,
                collectionPath = "historicalWorkoutNames",
                localEntities = historicalWorkoutNamesDao.getAll().associateBy { it.id },
                onInsert = { historicalWorkoutNamesDao.insert(it as HistoricalWorkoutName) },
                onRequestId = { (it as HistoricalWorkoutName).id.toString() },
            )

            Log.d(TAG, "Sync completed for user $userId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync for user $userId: ${e.message}", e)
            // Handle errors appropriately (e.g., retry logic, user notification)
        }
    }

    private suspend fun syncEntities(
        userId: String,
        collectionPath: String,
        localEntities: Map<Long, BaseEntity>,
        onInsert: suspend (BaseEntity) -> Unit,
        onRequestId: (BaseEntity) -> String,
    ) {
        val firestoreWorkoutsCollection =
            firestore.collection("users").document(userId).collection(collectionPath)
        val cloudWorkoutsSnapshot = firestoreWorkoutsCollection.get().await()

        // Update out of date workouts and insert new ones
        cloudWorkoutsSnapshot.forEach { firestoreWorkoutSnapshot ->
            val firestoreWorkout = firestoreWorkoutSnapshot.toObject<Workout>()
            val localEntity = localEntities[firestoreWorkout.id]
            val localEntityLastModified = localEntity?.lastUpdated ?: Date(0)

            if (localEntity == null || localEntityLastModified < firestoreWorkout.lastUpdated) {
                onInsert(firestoreWorkout)
            }
        }

        val unsyncedWorkouts = localEntities.values.filter { !it.synced }
        if (!unsyncedWorkouts.isEmpty()) {
            val batch = firestore.batch()
            unsyncedWorkouts.forEach { unsyncedWorkout ->
                val workoutRef = firestore
                    .collection("users")
                    .document(userId)
                    .collection(collectionPath)
                    .document(onRequestId(unsyncedWorkout))

                batch.set(workoutRef, unsyncedWorkout)
            }
            batch.commit().await()
        }
    }
}