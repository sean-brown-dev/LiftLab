package com.browntowndev.liftlab.core.persistence.room.repositories


import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.domain.models.WorkoutInProgress
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WorkoutInProgressRepositoryImpl(
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val firestoreSyncManager: FirestoreSyncManager,
): WorkoutInProgressRepository {
    override suspend fun getAll(): List<WorkoutInProgress> {
        TODO("Not yet implemented")
    }

    override suspend fun getById(id: Long): WorkoutInProgress? {
        TODO("Not yet implemented")
    }

    override suspend fun getMany(ids: List<Long>): List<WorkoutInProgress> {
        TODO("Not yet implemented")
    }

    override suspend fun update(model: WorkoutInProgress) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMany(models: List<WorkoutInProgress>) {
        TODO("Not yet implemented")
    }

    override suspend fun upsert(model: WorkoutInProgress): Long {
        TODO("Not yet implemented")
    }

    override suspend fun upsertMany(models: List<WorkoutInProgress>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun insert(model: WorkoutInProgress): Long {
        // Delete any that exist. Just calling delete because selecting then checking for null
        // still results in 1 SQL query anyway
        deleteAll()

        val toInsert =
            WorkoutInProgressEntity(
                workoutId = model.workoutId,
                startTime = model.startTime,
            )
        val id = workoutInProgressDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )
        return id
    }

    override suspend fun insertMany(models: List<WorkoutInProgress>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(model: WorkoutInProgress): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMany(models: List<WorkoutInProgress>): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteById(id: Long): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAll(): Int {
        val toDelete = workoutInProgressDao.get() ?: return 0
        val deleteCount = workoutInProgressDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }

        return deleteCount
    }

    override suspend fun getWithoutCompletedSets(): WorkoutInProgress? {
        return workoutInProgressDao.get()?.let { inProgressEntity ->
            WorkoutInProgress(
                workoutId = inProgressEntity.workoutId,
                startTime = inProgressEntity.startTime,
                completedSets = listOf()
            )
        }
    }

    override suspend fun getFlow(mesoCycle: Int, microCycle: Int): Flow<WorkoutInProgress?> {
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