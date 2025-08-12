package com.browntowndev.liftlab.core.data.local.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.Date

class WorkoutLogRepositoryImpl(
    private val workoutLogEntryDao: WorkoutLogEntryDao,
    private val setLogEntryDao: SetLogEntryDao,
    private val syncScheduler: SyncScheduler,
) : WorkoutLogRepository {

    override suspend fun getAll(): List<WorkoutLogEntry> =
        workoutLogEntryDao.getAll().fastMap { it.toDomainModel() }

    override fun getAllFlow(): Flow<List<WorkoutLogEntry>> {
        return workoutLogEntryDao.getAllFlattenedFlow().map {
            it.toDomainModel()
        }
    }

    override suspend fun getById(id: Long): WorkoutLogEntry? =
        workoutLogEntryDao.get(id)?.toDomainModel()

    override suspend fun getMany(ids: List<Long>): List<WorkoutLogEntry> =
        workoutLogEntryDao.getMany(ids).fastMap { it.toDomainModel() }

    override suspend fun update(model: WorkoutLogEntry) {
        val current = workoutLogEntryDao.get(model.id) ?: return
        val toUpdate = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        workoutLogEntryDao.update(toUpdate)

        val currentSetLogs = setLogEntryDao.getForWorkoutLogEntry(model.id)
            .associateBy { it.id }
        val toUpdateSetLogs = model.setLogEntries.map {
            it.toEntity().applyRemoteStorageMetadata(
                remoteId = currentSetLogs[it.id]?.remoteId,
                remoteLastUpdated = currentSetLogs[it.id]?.remoteLastUpdated,
                synced = false,
            )
        }
        setLogEntryDao.updateMany(toUpdateSetLogs)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<WorkoutLogEntry>) {
        TODO("Not yet implemented")
    }

    override suspend fun upsert(model: WorkoutLogEntry): Long {
        TODO("Not yet implemented")
    }

    override suspend fun upsertMany(models: List<WorkoutLogEntry>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun insert(model: WorkoutLogEntry): Long {
        TODO("Not yet implemented")
    }

    override suspend fun insertMany(models: List<WorkoutLogEntry>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(model: WorkoutLogEntry): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMany(models: List<WorkoutLogEntry>): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteById(id: Long): Int {
        val deleteCount =  workoutLogEntryDao.softDelete(id)
        if (deleteCount > 0) {
            syncScheduler.scheduleSync()
        }

        return deleteCount
    }

    override fun getFlow(workoutLogEntryId: Long): Flow<WorkoutLogEntry> {
        return workoutLogEntryDao.getFlattenedFlow(workoutLogEntryId = workoutLogEntryId)
            .mapNotNull { flattenedLogEntries ->
                flattenedLogEntries.toDomainModel().firstOrNull()
            }
    }

    override fun getWorkoutLogsForLiftFlow(liftId: Long): Flow<List<WorkoutLogEntry>> {
        return workoutLogEntryDao.getLogsByLiftIdFlow(liftId)
            .mapNotNull { flattenedLogEntries ->
                flattenedLogEntries.toDomainModel()
            }
    }

    private suspend fun getMostRecentLogsForLiftIds(
        liftIds: List<Long>,
        includeDeloads: Boolean
    ): List<WorkoutLogEntry> =
        workoutLogEntryDao.getMostRecentLogsForLiftIds(liftIds, includeDeloads)
            .toDomainModel()

    override suspend fun getMostRecentSetResultsForLiftIds(
        liftIds: List<Long>,
        includeDeloads: Boolean,
    ): List<SetLogEntry> {
        return getMostRecentLogsForLiftIds(liftIds, includeDeloads)
            .flatMap { workoutLog ->
                workoutLog.setLogEntries
            }
    }

    override suspend fun getMostRecentSetResultsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        date: Date,
    ): List<SetLogEntry> {
        return workoutLogEntryDao.getMostRecentLogsForLiftIdsPriorToDate(liftIds, date)
            .toDomainModel()
            .flatMap { workoutLog ->
                workoutLog.setLogEntries
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
                mesoCycle = mesoCycle,
                microCycle = microCycle,
                microcyclePosition = microcyclePosition,
                date = date,
                durationInMillis = durationInMillis,
            )
        val id = workoutLogEntryDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }
}