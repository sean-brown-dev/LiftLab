package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper

class WorkoutLiftsRepository (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val workoutLiftMapper: WorkoutLiftMapper,
): Repository {

    suspend fun insert(workoutLift: GenericWorkoutLift): Long {
        return workoutLiftsDao.insert(workoutLiftMapper.map(workoutLift))
    }

    suspend fun insertAll(workoutLifts: List<GenericWorkoutLift>): List<Long> {
        return workoutLiftsDao.insertMany(workoutLifts.map { workoutLiftMapper.map(it) })
    }

    suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long) {
        workoutLiftsDao.updateLiftId(workoutLiftId, newLiftId)
    }

    suspend fun update(workoutLift: GenericWorkoutLift) {
        val current = workoutLiftsDao.get(workoutLift.id)
        workoutLiftsDao.update(
            workoutLiftMapper.map(workoutLift).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        )
    }

    suspend fun updateMany(workoutLifts: List<GenericWorkoutLift>) {
        val currentEntities = workoutLiftsDao.getMany(workoutLifts.map { it.id }).associateBy { it.id }
        workoutLiftsDao.updateMany(
            workoutLifts.fastMap { workoutLift ->
                val current = currentEntities[workoutLift.id]
                workoutLiftMapper.map(workoutLift).copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false,
                )
            }
        )
    }

    suspend fun delete(workoutLift: GenericWorkoutLift) {
        workoutLiftsDao.delete(workoutLiftMapper.map(workoutLift))
    }

    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long> {
        return workoutLiftsDao.getLiftIdsForWorkout(workoutId)
    }

    suspend fun getForWorkout(workoutId: Long): List<GenericWorkoutLift> {
        return workoutLiftsDao.getForWorkout(workoutId).map { liftEntity ->
            workoutLiftMapper.map(liftEntity)
        }
    }
}