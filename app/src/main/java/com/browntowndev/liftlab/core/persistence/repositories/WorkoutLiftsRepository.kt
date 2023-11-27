package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper

class WorkoutLiftsRepository (
    private val workoutLiftsDao: WorkoutLiftsDao,
    private val workoutLiftMapper: WorkoutLiftMapper,
): Repository {

    suspend fun insert(workoutLift: GenericWorkoutLift): Long {
        return workoutLiftsDao.insert(workoutLiftMapper.map(workoutLift))
    }

    suspend fun insertAll(workoutLifts: List<GenericWorkoutLift>): List<Long> {
        return workoutLiftsDao.insertAll(workoutLifts.map { workoutLiftMapper.map(it) })
    }

    suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long) {
        workoutLiftsDao.updateLiftId(workoutLiftId, newLiftId)
    }

    suspend fun update(workoutLift: GenericWorkoutLift) {
        workoutLiftsDao.update(workoutLiftMapper.map(workoutLift))
    }

    suspend fun updateMany(workoutLifts: List<GenericWorkoutLift>) {
        workoutLiftsDao.updateMany(workoutLifts.map { workoutLiftMapper.map(it) })
    }

    suspend fun delete(workoutLift: GenericWorkoutLift) {
        workoutLiftsDao.delete(workoutLiftMapper.map(workoutLift))
    }

    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long> {
        return workoutLiftsDao.getLiftIdsForWorkout(workoutId)
    }

    suspend fun updateNote(workoutLiftId: Long, note: String?) {
        workoutLiftsDao.updateNote(workoutLiftId, note)
    }
}