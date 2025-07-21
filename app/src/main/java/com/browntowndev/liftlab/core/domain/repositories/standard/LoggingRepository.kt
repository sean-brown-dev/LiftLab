package com.browntowndev.liftlab.core.domain.repositories.standard

import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.room.dtos.PersonalRecordDto
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface LoggingRepository {
    fun getAllFlow(): Flow<List<WorkoutLogEntry>>
    suspend fun get(workoutLogEntryId: Long): WorkoutLogEntry?
    suspend fun getWorkoutLogsForLift(liftId: Long): List<WorkoutLogEntry>
    suspend fun getMostRecentSetResultsForLiftIds(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        includeDeload: Boolean,
    ): List<SetResult>

    suspend fun getMostRecentSetResultsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        date: Date,
    ): List<SetResult>

    suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecordDto>
    suspend fun insertFromPreviousSetResults(
        workoutLogEntryId: Long,
        workoutId: Long,
        mesocycle: Int,
        microcycle: Int,
        excludeFromCopy: List<Long>
    )

    suspend fun insertWorkoutLogEntry(
        historicalWorkoutNameId: Long,
        programDeloadWeek: Int,
        programWorkoutCount: Int,
        mesoCycle: Int,
        microCycle: Int,
        microcyclePosition: Int,
        date: Date,
        durationInMillis: Long,
    ): Long

    suspend fun deleteWorkoutLogEntry(workoutLogEntryId: Long)
    suspend fun deleteSetLogEntryById(id: Long)
    suspend fun upsert(workoutLogEntryId: Long, setLogEntry: SetLogEntry): Long
    suspend fun upsertMany(workoutLogEntryId: Long, setLogEntries: List<SetLogEntry>): List<Long>
}