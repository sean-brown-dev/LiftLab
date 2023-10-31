package com.browntowndev.liftlab.core.persistence.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLogEntryMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Date

class LoggingRepository(
    private val loggingDao: LoggingDao,
    private val workoutLogEntryMapper: WorkoutLogEntryMapper
): Repository {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAll(): LiveData<List<WorkoutLogEntryDto>> {
        return loggingDao.getAll().flatMapLatest {
            flowOf(workoutLogEntryMapper.map(it))
        }.asLiveData()
    }

    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): WorkoutLogEntryDto? {
        val log = loggingDao.getForWorkout(workoutId, mesoCycle, microCycle)
        return workoutLogEntryMapper.map(log).singleOrNull()
    }

    suspend fun getWorkoutLogsForLift(liftId: Long): List<WorkoutLogEntryDto> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> = loggingDao.getLogsByLiftId(liftId)
        return workoutLogEntryMapper.map(flattenedLogEntries)
    }

    suspend fun insertFromPreviousSetResults(workoutLogEntryId: Long, workoutId: Long, excludeFromCopy: List<Long>) {
        loggingDao.insertFromPreviousSetResults(workoutLogEntryId, workoutId, excludeFromCopy)
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