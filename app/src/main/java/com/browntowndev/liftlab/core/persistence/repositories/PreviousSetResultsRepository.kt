package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
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

    suspend fun getPersonalRecordsForLiftsExcludingWorkout(
        workoutId: Long,
        mesoCycle: Int, microCycle: Int,
        liftIds: List<Long>
    ): List<PersonalRecordDto> {
        return previousSetResultDao.getPersonalRecordsForLiftsExcludingWorkout(
            workoutId = workoutId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftIds = liftIds,
        )
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

    suspend fun deleteById(id: Long) {
        previousSetResultDao.deleteById(id)
    }
}
