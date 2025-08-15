package com.browntowndev.liftlab.core.data.local.repositories


import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.sync.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutInProgress
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

class WorkoutInProgressRepositoryImpl(
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val syncScheduler: SyncScheduler,
): WorkoutInProgressRepository {
    override suspend fun isWorkoutInProgress(workoutId: Long): Boolean =
        workoutInProgressDao.getByWorkoutId(workoutId) != null

    override suspend fun getWithoutCompletedSets(): WorkoutInProgress? {
        return workoutInProgressDao.get()?.let { inProgressEntity ->
            WorkoutInProgress(
                workoutId = inProgressEntity.workoutId,
                startTime = inProgressEntity.startTime,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFlow(mesoCycle: Int, microCycle: Int): Flow<WorkoutInProgress?> {
        return workoutInProgressDao.getFlow().mapLatest { inProgressWorkout ->
            if (inProgressWorkout == null) null
            else {
                WorkoutInProgress(
                    workoutId = inProgressWorkout.workoutId,
                    startTime = inProgressWorkout.startTime,
                )
            }
        }
    }

    override suspend fun getAll(): List<WorkoutInProgress> {
        return workoutInProgressDao.getAll().map {
            WorkoutInProgress(
                workoutId = it.workoutId,
                startTime = it.startTime,
            )
        }
    }

    override fun getAllFlow(): Flow<List<WorkoutInProgress>> {
        return workoutInProgressDao.getAllFlow().map {
            it.map { entity ->
                WorkoutInProgress(
                    workoutId = entity.workoutId,
                    startTime = entity.startTime,
                )
            }
        }
    }

    override suspend fun getById(id: Long): WorkoutInProgress? {
        return workoutInProgressDao.get(id)?.let {
            WorkoutInProgress(
                workoutId = it.workoutId,
                startTime = it.startTime,
            )
        }
    }

    override suspend fun getMany(ids: List<Long>): List<WorkoutInProgress> {
        return workoutInProgressDao.getMany(ids).map {
            WorkoutInProgress(
                workoutId = it.workoutId,
                startTime = it.startTime,
            )
        }
    }

    override suspend fun update(model: WorkoutInProgress) {
        val existing = workoutInProgressDao.get(model.workoutId) ?: return
        val toUpdate = WorkoutInProgressEntity(
            id = model.workoutId,
            workoutId = model.workoutId,
            startTime = model.startTime,
        ).applyRemoteStorageMetadata(
            remoteId = existing.remoteId,
            remoteLastUpdated = existing.remoteLastUpdated,
            synced = false
        )
        workoutInProgressDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<WorkoutInProgress>) {
        val existingById = workoutInProgressDao.getMany(models.map { it.workoutId }).associateBy { it.id }
        val toUpdate = models.map {
            WorkoutInProgressEntity(
                id = it.workoutId,
                workoutId = it.workoutId,
                startTime = it.startTime,
            ).applyRemoteStorageMetadata(
                remoteId = existingById[it.workoutId]?.remoteId,
                remoteLastUpdated = existingById[it.workoutId]?.remoteLastUpdated,
                synced = false
            )
        }
        workoutInProgressDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: WorkoutInProgress): Long {
        val existing = workoutInProgressDao.get(model.workoutId)
        val toUpsert = WorkoutInProgressEntity(
            id = model.workoutId,
            workoutId = model.workoutId,
            startTime = model.startTime,
        ).applyRemoteStorageMetadata(
            remoteId = existing?.remoteId,
            remoteLastUpdated = existing?.remoteLastUpdated,
            synced = false
        )
        val id = workoutInProgressDao.upsert(toUpsert)
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<WorkoutInProgress>): List<Long> {
        val existingById = workoutInProgressDao.getMany(models.map { it.workoutId }).associateBy { it.id }
        val toUpsert = models.map {
            WorkoutInProgressEntity(
                id = it.workoutId,
                workoutId = it.workoutId,
                startTime = it.startTime,
            ).applyRemoteStorageMetadata(
                remoteId = existingById[it.workoutId]?.remoteId,
                remoteLastUpdated = existingById[it.workoutId]?.remoteLastUpdated,
                synced = false
            )
        }
        val ids = workoutInProgressDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

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
        syncScheduler.scheduleSync()

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
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun delete(model: WorkoutInProgress): Int {
        val count = workoutInProgressDao.softDelete(model.workoutId)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<WorkoutInProgress>): Int {
        val ids = models.map { it.workoutId }
        if (ids.isEmpty()) return 0
        val count = workoutInProgressDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val count = workoutInProgressDao.softDelete(id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteAll(): Int {
        val toDelete = workoutInProgressDao.getAll()
        if (toDelete.isEmpty()) return 0
        val deleteCount = workoutInProgressDao.softDeleteMany(toDelete.fastMap { it.id })
        if (deleteCount > 0) {
            syncScheduler.scheduleSync()
        }
        return deleteCount
    }
}