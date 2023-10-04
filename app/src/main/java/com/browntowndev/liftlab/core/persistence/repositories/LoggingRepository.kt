package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLogEntryMapper

class LoggingRepository(
    private val loggingDao: LoggingDao,
    private val workoutLogEntryMapper: WorkoutLogEntryMapper
): Repository {
    suspend fun getWorkoutLogsForLift(liftId: Long) {

    }
}