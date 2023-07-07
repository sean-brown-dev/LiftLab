package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName

class HistoricalWorkoutNamesRepository(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao
): Repository {

    suspend fun insert(programId: Long, workoutId: Long, programName: String, workoutName: String): Long {
        return historicalWorkoutNamesDao.insert(
            HistoricalWorkoutName(
                programId = programId,
                workoutId = workoutId,
                programName = programName,
                workoutName = workoutName,
            )
        )
    }

    suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long? {
        return historicalWorkoutNamesDao.getByProgramAndWorkoutId(programId, workoutId)?.id
    }
}