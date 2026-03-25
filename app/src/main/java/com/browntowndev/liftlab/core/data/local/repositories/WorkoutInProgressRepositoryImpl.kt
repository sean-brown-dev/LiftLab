package com.browntowndev.liftlab.core.data.local.repositories


import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutInProgress
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutInProgressRepositoryImpl(
    private val workoutInProgressDao: WorkoutInProgressDao,
    private val syncScheduler: SyncScheduler,
): WorkoutInProgressRepository {
    override suspend fun isWorkoutInProgress(workoutId: Long): Boolean =
        workoutInProgressDao.get()?.workoutId == workoutId

    override suspend fun get(): WorkoutInProgress? =
        workoutInProgressDao.get()?.let { inProgressWorkout ->
            WorkoutInProgress(
                workoutId = inProgressWorkout.workoutId,
                startTime = inProgressWorkout.startTime,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFlow(): Flow<WorkoutInProgress?> {
        return workoutInProgressDao.getFlow().map { inProgressWorkout ->
            if (inProgressWorkout == null) null
            else {
                WorkoutInProgress(
                    workoutId = inProgressWorkout.workoutId,
                    startTime = inProgressWorkout.startTime,
                )
            }
        }
    }

    override suspend fun upsert(model: WorkoutInProgress): Long {
        val existing = workoutInProgressDao.get()
        val toUpsert = WorkoutInProgressEntity(
            workoutId = model.workoutId,
            startTime = model.startTime,
        ).applyRemoteStorageMetadata(
            remoteId = existing?.remoteId,
            remoteLastUpdated = existing?.remoteLastUpdated,
            synced = false,
        )
        val id = workoutInProgressDao.upsert(toUpsert)
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun delete(): Int {
        val deleteCount = workoutInProgressDao.softDelete()
        if (deleteCount > 0) {
            syncScheduler.scheduleSync()
        }

        return deleteCount
    }
}