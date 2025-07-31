package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface WorkoutLogRepository: Repository<WorkoutLogEntry, Long> {
    override suspend fun getAll(): List<WorkoutLogEntry>
    override fun getAllFlow(): Flow<List<WorkoutLogEntry>>
    fun getFlow(workoutLogEntryId: Long): Flow<WorkoutLogEntry>
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
}