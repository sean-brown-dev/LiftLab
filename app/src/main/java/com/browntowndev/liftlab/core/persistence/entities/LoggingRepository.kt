package com.browntowndev.liftlab.core.persistence.entities

import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.repositories.Repository
import java.util.Date

class LoggingRepository(private val loggingDao: LoggingDao): Repository {
    suspend fun insertFromPreviousSetResults(workoutLogEntryId: Long) {
        loggingDao.insertFromPreviousSetResults(workoutLogEntryId)
    }

    suspend fun insertWorkoutLogEntry(
        historicalWorkoutNameId: Long,
        mesoCycle: Int,
        microCycle: Int,
        date: Date,
        durationInMillis: Long,
    ): Long {
        return loggingDao.insert(
            WorkoutLogEntry(
                historicalWorkoutNameId = historicalWorkoutNameId,
                mesocycle = mesoCycle,
                microcycle = microCycle,
                date = date,
                durationInMillis = durationInMillis,
            )
        )
    }
}