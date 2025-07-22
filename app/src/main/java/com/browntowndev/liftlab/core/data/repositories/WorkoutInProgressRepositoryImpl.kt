package com.browntowndev.liftlab.core.data.repositories


import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.domain.models.WorkoutInProgress
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.data.remote.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.data.remote.sync.SyncQueueEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WorkoutInProgressRepositoryImpl(
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val firestoreSyncManager: FirestoreSyncManager,
): WorkoutInProgressRepository {
    override suspend fun getAll(): List<WorkoutInProgress> {
        return workoutInProgressDao.getAll().map {
            WorkoutInProgress(
                workoutId = it.workoutId,
                startTime = it.startTime,
                completedSets = emptyList()
            )
        }
    }

    override suspend fun getById(id: Long): WorkoutInProgress? {
        return workoutInProgressDao.get(id)?.let {
            WorkoutInProgress(
                workoutId = it.workoutId,
                startTime = it.startTime,
                completedSets = emptyList()
            )
        }
    }

    override suspend fun getMany(ids: List<Long>): List<WorkoutInProgress> {
        return workoutInProgressDao.getMany(ids).map {
            WorkoutInProgress(
                workoutId = it.workoutId,
                startTime = it.startTime,
                completedSets = emptyList()
            )
        }
    }

    override suspend fun update(model: WorkoutInProgress) {
        val toUpdate = WorkoutInProgressEntity(
            id = model.workoutId,
            workoutId = model.workoutId,
            startTime = model.startTime,
        )
        workoutInProgressDao.update(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert
            )
        )
    }

    override suspend fun updateMany(models: List<WorkoutInProgress>) {
        val toUpdate = models.map {
            WorkoutInProgressEntity(
                id = it.workoutId,
                workoutId = it.workoutId,
                startTime = it.startTime,
            )
        }
        workoutInProgressDao.updateMany(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                roomEntityIds = toUpdate.map { it.id },
                SyncType.Upsert
            )
        )
    }

    override suspend fun upsert(model: WorkoutInProgress): Long {
        val toUpsert = WorkoutInProgressEntity(
            id = model.workoutId,
            workoutId = model.workoutId,
            startTime = model.startTime,
        )
        val id = workoutInProgressDao.upsert(toUpsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                roomEntityIds = listOf(if (id == -1L) toUpsert.id else id),
                SyncType.Upsert
            )
        )
        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<WorkoutInProgress>): List<Long> {
        val toUpsert = models.map {
            WorkoutInProgressEntity(
                id = it.workoutId,
                workoutId = it.workoutId,
                startTime = it.startTime,
            )
        }
        val ids = workoutInProgressDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }.fastMap { it.id }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                roomEntityIds = entityIds,
                SyncType.Upsert
            )
        )
        return entityIds
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
        deleteAll()
        val toInsert = models.map {
            WorkoutInProgressEntity(
                workoutId = it.workoutId,
                startTime = it.startTime,
            )
        }
        val ids = workoutInProgressDao.insertMany(toInsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                roomEntityIds = ids,
                SyncType.Upsert
            )
        )
        return ids
    }

    override suspend fun delete(model: WorkoutInProgress): Int {
        val toDelete = workoutInProgressDao.get(model.workoutId) ?: return 0
        val count = workoutInProgressDao.delete(toDelete)
        if (count > 0 && toDelete.remoteId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteMany(models: List<WorkoutInProgress>): Int {
        val toDelete = models.map {
            WorkoutInProgressEntity(
                id = it.workoutId,
                workoutId = it.workoutId,
                startTime = it.startTime
            )
        }
        val count = workoutInProgressDao.deleteMany(toDelete)
        val firestoreIds = toDelete.mapNotNull { it.remoteId }
        if (firestoreIds.isNotEmpty() && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                    roomEntityIds = toDelete.map { it.id },
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = workoutInProgressDao.get(id) ?: return 0
        val count = workoutInProgressDao.delete(toDelete)
        if (count > 0 && toDelete.remoteId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteAll(): Int {
        val toDelete = workoutInProgressDao.get() ?: return 0
        val deleteCount = workoutInProgressDao.delete(toDelete)

        if (toDelete.remoteId != null) {
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