package com.browntowndev.liftlab.core.domain.repositories.standard


import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.domain.models.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WorkoutInProgressRepository(
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun insert(workoutInProgress: WorkoutInProgress) {
        // Delete any that exist. Just calling delete because selecting then checking for null
        // still results in 1 SQL query anyway
        delete()

        val toInsert =
            WorkoutInProgressEntity(
                workoutId = workoutInProgress.workoutId,
                startTime = workoutInProgress.startTime,
            )
        val id = workoutInProgressDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )
    }

    suspend fun delete() {
        val toDelete = workoutInProgressDao.get() ?: return
        workoutInProgressDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
    }

    suspend fun getWithoutCompletedSets(): WorkoutInProgress? {
        return workoutInProgressDao.get()?.let { inProgressEntity ->
            WorkoutInProgress(
                workoutId = inProgressEntity.workoutId,
                startTime = inProgressEntity.startTime,
                completedSets = listOf()
            )
        }
    }

    suspend fun getFlow(mesoCycle: Int, microCycle: Int): Flow<WorkoutInProgress?> {
        return workoutInProgressDao.get().let { inProgressWorkout ->
            if (inProgressWorkout == null) {
                return flowOf(null)
            }

            previousSetResultsRepository.getForWorkoutFlow(
                workoutId = inProgressWorkout.workoutId,
                mesoCycle = mesoCycle,
                microCycle = microCycle,
            ).map { completedSets ->
                WorkoutInProgress(
                    workoutId = inProgressWorkout.workoutId,
                    startTime = inProgressWorkout.startTime,
                    completedSets = completedSets,
                )
            }
        }
    }
}