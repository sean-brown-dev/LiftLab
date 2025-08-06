package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface WorkoutLogRepository: Repository<WorkoutLogEntry, Long> {
    override suspend fun getAll(): List<WorkoutLogEntry>
    override fun getAllFlow(): Flow<List<WorkoutLogEntry>>
    fun getFlow(workoutLogEntryId: Long): Flow<WorkoutLogEntry>
    suspend fun getMostRecentSetResultsForLiftIds(
        liftIds: List<Long>,
        includeDeloads: Boolean,
    ): List<SetLogEntry>

    suspend fun getMostRecentSetResultsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        date: Date,
    ): List<SetLogEntry>

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
    fun getWorkoutLogsForLiftFlow(liftId: Long): Flow<List<WorkoutLogEntry>>
}