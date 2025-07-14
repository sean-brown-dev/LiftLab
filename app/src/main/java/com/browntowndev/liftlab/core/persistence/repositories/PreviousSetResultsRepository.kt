package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
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
        val current = previousSetResultDao.get(setResult.id)
        val toUpsert = setResultsMapper.map(setResult)
            .copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false
            )
        return previousSetResultDao.upsert(toUpsert)
    }

    suspend fun upsertMany(setResults: List<SetResult>): List<Long> {
        val currentEntities = previousSetResultDao.getMany(setResults.map { it.id }).associateBy { it.id }
        return previousSetResultDao.upsertMany(
            setResults.fastMap { setResult ->
                val current = currentEntities[setResult.id]
                setResultsMapper.map(setResult).copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
            }
        )
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
