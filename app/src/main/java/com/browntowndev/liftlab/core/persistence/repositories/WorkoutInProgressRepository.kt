package com.browntowndev.liftlab.core.persistence.repositories


import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope

class WorkoutInProgressRepository(
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
): Repository {
    suspend fun insert(workoutInProgress: WorkoutInProgressDto) {
        // Delete any that exist. Just calling delete because selecting then checking for null
        // still results in 1 SQL query anyway
        delete()

        val toInsert =
            WorkoutInProgress(
                workoutId = workoutInProgress.workoutId,
                startTime = workoutInProgress.startTime,
            )
        val id = workoutInProgressDao.insert(toInsert)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                entity = toInsert.toFirestoreDto().copy(id = id),
                onSynced = {
                    workoutInProgressDao.update(it.toEntity())
                }
            )
        }
    }

    suspend fun delete() {
        val toDelete = workoutInProgressDao.get() ?: return
        workoutInProgressDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            syncScope.fireAndForgetSync {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!,
                )
            }
        }
    }

    suspend fun getWithoutCompletedSets(): WorkoutInProgressDto? {
        return workoutInProgressDao.get()?.let { inProgressEntity ->
            WorkoutInProgressDto(
                workoutId = inProgressEntity.workoutId,
                startTime = inProgressEntity.startTime,
                completedSets = listOf()
            )
        }
    }

    suspend fun get(mesoCycle: Int, microCycle: Int): WorkoutInProgressDto? {
        return workoutInProgressDao.get()?.let { inProgressWorkout ->
            val completedSets = previousSetResultsRepository.getForWorkout(
                workoutId = inProgressWorkout.workoutId,
                mesoCycle = mesoCycle,
                microCycle = microCycle,
            )

            WorkoutInProgressDto(
                workoutId = inProgressWorkout.workoutId,
                startTime = inProgressWorkout.startTime,
                completedSets = completedSets,
            )
        }
    }
}