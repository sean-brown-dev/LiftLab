package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.persistence.dao.LoggingDao
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLogEntryMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Date

class LoggingRepository(
    private val loggingDao: LoggingDao,
    private val workoutLogEntryMapper: WorkoutLogEntryMapper,
    private val setResultMapper: SetResultMapper,
): Repository {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAll(): LiveData<List<WorkoutLogEntryDto>> {
        return loggingDao.getAll().flatMapLatest {
            flowOf(workoutLogEntryMapper.map(it))
        }.asLiveData()
    }

    suspend fun get(workoutLogEntryId: Long): WorkoutLogEntryDto? {
        val log: List<FlattenedWorkoutLogEntryDto> = loggingDao.get(workoutLogEntryId = workoutLogEntryId)
        return workoutLogEntryMapper.map(log).singleOrNull()
    }

    suspend fun getWorkoutLogsForLift(liftId: Long): List<WorkoutLogEntryDto> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> = loggingDao.getLogsByLiftId(liftId)
        return workoutLogEntryMapper.map(flattenedLogEntries)
    }

    private suspend fun getMostRecentLogsForLiftIds(liftIds: List<Long>, includeDeload: Boolean): List<WorkoutLogEntryDto> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> = if(includeDeload) {
            loggingDao.getMostRecentLogsForLiftIds(liftIds)
        } else {
            loggingDao.getMostRecentLogsForLiftIdsExcludingDeloads(liftIds)
        }

        return workoutLogEntryMapper.map(flattenedLogEntries)
    }

    suspend fun getMostRecentSetResultsForLiftIds(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return getMostRecentLogsForLiftIds(liftIds, includeDeload)
            .flatMap { workoutLog ->
                workoutLog.setResults.fastMap { setLogEntry ->
                    setResultMapper.map(
                        from = setLogEntry,
                        workoutId = workoutLog.workoutId,
                        isLinearProgression = linearProgressionLiftIds.contains(
                            setLogEntry.liftId
                        )
                    )
                }
            }
    }

    suspend fun getMostRecentSetResultsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        date: Date,
    ): List<SetResult> {
        return workoutLogEntryMapper.map(
            loggingDao.getMostRecentLogsForLiftIdsPriorToDate(liftIds, date)
        ).flatMap { workoutLog ->
            workoutLog.setResults.fastMap { setLogEntry ->
                setResultMapper.map(
                    from = setLogEntry,
                    workoutId = workoutLog.workoutId,
                    isLinearProgression = linearProgressionLiftIds.contains(
                        setLogEntry.liftId
                    )
                )
            }
        }
    }

    suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecordDto> {
        return loggingDao.getPersonalRecordsForLifts(liftIds)
    }

    suspend fun insertFromPreviousSetResults(
        workoutLogEntryId: Long,
        workoutId: Long,
        mesocycle: Int,
        microcycle: Int,
        excludeFromCopy: List<Long>
    ) {
        loggingDao.insertFromPreviousSetResults(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = workoutId,
            mesocycle = mesocycle,
            microcycle = microcycle,
            excludeFromCopy = excludeFromCopy,
        )
    }

    suspend fun insertWorkoutLogEntry(
        historicalWorkoutNameId: Long,
        programDeloadWeek: Int,
        programWorkoutCount: Int,
        mesoCycle: Int,
        microCycle: Int,
        microcyclePosition: Int,
        date: Date,
        durationInMillis: Long,
    ): Long {
        return loggingDao.insert(
            WorkoutLogEntry(
                historicalWorkoutNameId = historicalWorkoutNameId,
                programDeloadWeek = programDeloadWeek,
                programWorkoutCount = programWorkoutCount,
                mesocycle = mesoCycle,
                microcycle = microCycle,
                microcyclePosition = microcyclePosition,
                date = date,
                durationInMillis = durationInMillis,
            )
        )
    }

    suspend fun deleteWorkoutLogEntry(workoutLogEntryId: Long) {
        loggingDao.deleteSetLogEntriesForWorkout(workoutLogEntryId = workoutLogEntryId)
        loggingDao.deleteWorkoutLogEntry(workoutLogEntryId = workoutLogEntryId)
    }

    suspend fun deleteSetLogEntryById(id: Long) {
        loggingDao.deleteSetLogEntryById(id)
    }

    suspend fun upsert(workoutLogEntryId: Long, setLogEntry: SetLogEntryDto): Long {
        return loggingDao.upsert(workoutLogEntryMapper.map(workoutLogEntryId, setLogEntry))
    }

    suspend fun upsertMany(workoutLogEntryId: Long, setLogEntries: List<SetLogEntryDto>): List<Long> {
        return loggingDao.upsertMany(
            setLogEntries.fastMap { setLogEntry ->
                workoutLogEntryMapper.map(workoutLogEntryId, setLogEntry)
            }
        )
    }
}