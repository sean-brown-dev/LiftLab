package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper

class PreviousSetResultsRepository(
    private val previousSetResultDao: PreviousSetResultDao,
    private val setResultsMapper: SetResultMapper,
): Repository {
    suspend fun getByWorkoutIdExcludingGivenMesoAndMicro(workoutId: Long, mesoCycle: Int, microCycle: Int): List<SetResult> {
        return previousSetResultDao.getByWorkoutIdExcludingGivenMesoAndMicro(workoutId, mesoCycle, microCycle).map {
            setResultsMapper.map(it)
        }
    }

    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<SetResult> {
        return previousSetResultDao.getForWorkout(workoutId, mesoCycle, microCycle).map {
            setResultsMapper.map(it)
        }
    }

    suspend fun upsert(setResult: SetResult): Long {
        return previousSetResultDao.upsert(setResultsMapper.map(setResult))
    }

    suspend fun insertMany(setResult: List<SetResult>) {
        previousSetResultDao.insertMany(setResult.map { setResultsMapper.map(it) })
    }

    suspend fun deleteAllNotForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        previousSetResultDao.deleteAllNotForWorkoutMesoAndMicro(workoutId, mesoCycle, microCycle)
    }

    suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        previousSetResultDao.deleteAllForWorkout(workoutId, mesoCycle, microCycle)
    }

    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int) {
        previousSetResultDao.delete(workoutId, liftId, setPosition)
    }

    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int, myoRepSetPosition: Int?) {
        previousSetResultDao.delete(workoutId, liftId, setPosition, myoRepSetPosition)
    }
}