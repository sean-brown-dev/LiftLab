package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper

class PreviousSetResultsRepository(
    private val previousSetResultDao: PreviousSetResultDao,
    private val setResultsMapper: SetResultMapper,
): Repository {
    suspend fun getByWorkoutId(workoutId: Long): List<SetResult> {
        return previousSetResultDao.getByWorkoutId(workoutId).map {
            setResultsMapper.map(it)
        }
    }

    suspend fun insert(setResult: SetResult) {
        previousSetResultDao.insert(setResultsMapper.map(setResult))
    }

    suspend fun deleteAllNotForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        previousSetResultDao.deleteAllNotForWorkout(workoutId, mesoCycle, microCycle)
    }

    suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        previousSetResultDao.deleteAllForWorkout(workoutId, mesoCycle, microCycle)
    }

    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int) {
        previousSetResultDao.delete(workoutId, liftId, setPosition)
    }

    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int, myoRepSetPosition: Int) {
        previousSetResultDao.delete(workoutId, liftId, setPosition, myoRepSetPosition)
    }
}