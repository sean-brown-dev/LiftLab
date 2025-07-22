package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.mapping.SetResultMappingExtensions.toSetResult
import com.browntowndev.liftlab.core.data.mapping.WorkoutLogEntryMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.data.local.dtos.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class WorkoutLogRepositoryImpl(
    private val workoutLogEntryDao: WorkoutLogEntryDao,
    private val setLogEntryDao: SetLogEntryDao,
    private val syncScheduler: SyncScheduler,
) : WorkoutLogRepository {

    override fun getAll(): List<WorkoutLogEntry> =
        workoutLogEntryDao.getAll().fastMap { it.toDomainModel() }

    override fun getAllFlow(): Flow<List<WorkoutLogEntry>> {
        return workoutLogEntryDao.getAllFlattened().map {
            it.toDomainModel()
        }
    }

    override suspend fun get(workoutLogEntryId: Long): WorkoutLogEntry? {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> =
            workoutLogEntryDao.getFlattened(workoutLogEntryId = workoutLogEntryId)
        return flattenedLogEntries.toDomainModel().firstOrNull()
    }

    override suspend fun getWorkoutLogsForLift(liftId: Long): List<WorkoutLogEntry> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> =
            workoutLogEntryDao.getLogsByLiftId(liftId)
        return flattenedLogEntries.toDomainModel()
    }

    private suspend fun getMostRecentLogsForLiftIds(
        liftIds: List<Long>,
        includeDeload: Boolean
    ): List<WorkoutLogEntry> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> = if (includeDeload) {
            workoutLogEntryDao.getMostRecentLogsForLiftIds(liftIds)
        } else {
            workoutLogEntryDao.getMostRecentLogsForLiftIdsExcludingDeloads(liftIds)
        }

        return flattenedLogEntries.toDomainModel()
    }

    override suspend fun getMostRecentSetResultsForLiftIds(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return getMostRecentLogsForLiftIds(liftIds, includeDeload)
            .flatMap { workoutLog ->
                workoutLog.setResults.fastMap { setLogEntry ->
                    setLogEntry.toSetResult(
                        workoutId = workoutLog.workoutId,
                        isLinearProgression = linearProgressionLiftIds.contains(
                            setLogEntry.liftId
                        )
                    )
                }
            }
    }

    override suspend fun getMostRecentSetResultsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        date: Date,
    ): List<SetResult> {
        return workoutLogEntryDao.getMostRecentLogsForLiftIdsPriorToDate(liftIds, date)
            .toDomainModel()
            .flatMap { workoutLog ->
                workoutLog.setResults.fastMap { setLogEntry ->
                    setLogEntry.toSetResult(
                        workoutId = workoutLog.workoutId,
                        isLinearProgression = linearProgressionLiftIds.contains(
                            setLogEntry.liftId
                        )
                    )
                }
            }
    }

    override suspend fun insertWorkoutLogEntry(
        historicalWorkoutNameId: Long,
        programDeloadWeek: Int,
        programWorkoutCount: Int,
        mesoCycle: Int,
        microCycle: Int,
        microcyclePosition: Int,
        date: Date,
        durationInMillis: Long,
    ): Long {
        val toInsert =
            WorkoutLogEntryEntity(
                historicalWorkoutNameId = historicalWorkoutNameId,
                programDeloadWeek = programDeloadWeek,
                programWorkoutCount = programWorkoutCount,
                mesocycle = mesoCycle,
                microcycle = microCycle,
                microcyclePosition = microcyclePosition,
                date = date,
                durationInMillis = durationInMillis,
            )
        val id = workoutLogEntryDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun deleteWorkoutLogEntry(workoutLogEntryId: Long) {
        val setLogEntriesToDelete = setLogEntryDao.getForWorkoutLogEntry(workoutLogEntryId)
        var deletedCount = 0
        if (setLogEntriesToDelete.isNotEmpty()) {
            deletedCount += setLogEntryDao.softDeleteMany(setLogEntriesToDelete.map { it.id })
        }
        deletedCount += workoutLogEntryDao.softDelete(workoutLogEntryId)
        if (deletedCount > 0) {
            syncScheduler.scheduleSync()
        }
    }
}