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

    suspend fun upsertMany(setResults: List<SetResult>): List<Long> {
        return previousSetResultDao.upsertMany(setResults.map { setResult -> setResultsMapper.map(setResult) })
    }

    suspend fun deleteAllForPreviousWorkout(
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        currentResultsToDeleteInstead: List<Long>,
    ) {
        previousSetResultDao.deleteAllForPreviousWorkout(
            workoutId = workoutId,
            currentMesocycle = currentMesocycle,
            currentMicrocycle = currentMicrocycle,
            currentResultsToDelete = currentResultsToDeleteInstead,
        )
    }

    suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        previousSetResultDao.deleteAllForWorkout(workoutId, mesoCycle, microCycle)
    }

    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int) {
        previousSetResultDao.delete(workoutId, liftId, setPosition)
    }

    suspend fun delete(workoutId: Long, liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) {
        previousSetResultDao.delete(workoutId, liftPosition, setPosition, myoRepSetPosition)
    }

    suspend fun update(liftId: Long, liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?, weight: Float, reps: Int, rpe: Float) {
        previousSetResultDao.update(
            liftId = liftId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            weight = weight,
            reps = reps,
            rpe = rpe,
        )
    }
}
