package com.browntowndev.liftlab.core.data.local.repositories

import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutLiftsRepositoryImpl (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val syncScheduler: SyncScheduler,
): WorkoutLiftsRepository {
    override suspend fun getAll(): List<GenericWorkoutLift> {
        return workoutLiftsDao.getAll().map { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<GenericWorkoutLift>> {
        return workoutLiftsDao.getAllFlow().map { it.map { entity -> entity.toDomainModel() } }
    }

    override suspend fun getById(id: Long): GenericWorkoutLift? {
        return workoutLiftsDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<GenericWorkoutLift> {
        return workoutLiftsDao.getMany(ids).map { it.toDomainModel() }
    }

    override suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long> {
        return workoutLiftsDao.getLiftIdsForWorkout(workoutId)
    }

    override suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift> {
        return workoutLiftsDao.getForWorkout(workoutId).map { liftEntity ->
            liftEntity.toDomainModel()
        }
    }

    override suspend fun changeFromLiftsToNewLift(newLiftId: Long, existingLiftIds: List<Long>) {
        workoutLiftsDao.changeFromLiftsToNewLift(newLiftId, existingLiftIds)
        syncScheduler.scheduleSync()
    }
}