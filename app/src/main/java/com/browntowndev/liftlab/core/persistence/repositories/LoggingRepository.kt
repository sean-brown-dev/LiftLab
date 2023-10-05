package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLogEntryMapper
import java.util.Date

class LoggingRepository(
    private val loggingDao: LoggingDao,
    private val workoutLogEntryMapper: WorkoutLogEntryMapper
): Repository {
    suspend fun getAll(): List<SetLogEntry> {
        return loggingDao.getAll()
    }

    suspend fun getWorkoutLogsForLift(liftId: Long): List<WorkoutLogEntryDto> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> =  loggingDao.getLogsByLiftId(liftId)
        return workoutLogEntryMapper.map(flattenedLogEntries)
    }

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