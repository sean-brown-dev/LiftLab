package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap

import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SetLogEntryRepositoryImpl(
    private val setLogEntryDao: SetLogEntryDao,
    private val syncScheduler: SyncScheduler,
): SetLogEntryRepository {

    override suspend fun getAllCompletionDataForWorkout(workoutId: Long): List<SetLogEntry> {
        return setLogEntryDao.getForAllWorkoutCompletions(workoutId).fastMap { it.toDomainModel() }
    }

    override suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecord> {
        return setLogEntryDao.getPersonalRecordsForLifts(liftIds).fastMap {
            PersonalRecord(
                liftId = it.liftId,
                personalRecord = it.personalRecord,
            )
        }
    }

    override fun getLatestForWorkout(workoutId: Long, includeDeload: Boolean): Flow<List<SetLogEntry>> {
        return setLogEntryDao.getLatestForWorkout(workoutId, includeDeload)
            .map { setLogEntryEntities ->
                setLogEntryEntities.fastMap { it.toDomainModel() }
            }
    }

    override fun getForSpecificWorkoutCompletionFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<SetLogEntry>> {
        return setLogEntryDao.getForSpecificWorkoutCompletionFlow(workoutId, mesoCycle, microCycle)
            .map { results ->
                results.fastMap { it.toDomainModel() }
            }
    }

    override suspend fun getAll(): List<SetLogEntry> =
        setLogEntryDao.getAll().fastMap { it.toDomainModel() }

    override fun getAllFlow(): Flow<List<SetLogEntry>> {
        return setLogEntryDao.getAllFlow().map { setLogEntries ->
            setLogEntries.fastMap { it.toDomainModel() }
        }
    }

    override suspend fun getById(id: Long): SetLogEntry? =
        setLogEntryDao.get(id)?.toDomainModel()

    override suspend fun getMany(ids: List<Long>): List<SetLogEntry> =
        setLogEntryDao.getMany(ids).fastMap { it.toDomainModel() }

    override suspend fun update(model: SetLogEntry) {
        setLogEntryDao.update(model.toEntity())
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<SetLogEntry>) {
        setLogEntryDao.updateMany(models.fastMap { it.toEntity() })
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: SetLogEntry): Long {
        val current = setLogEntryDao.get(model.id)
        val toUpsert = model.toEntity()
            .applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.remoteLastUpdated,
                synced = false
            )

        val id = setLogEntryDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<SetLogEntry>): List<Long> {
        val currentEntries = setLogEntryDao.getMany(models.fastMap { it.id })
            .associateBy { it.id }
        val toUpsert = models.fastMap { setLogEntry ->
            val current = currentEntries[setLogEntry.id]
            setLogEntry.toEntity()
                .applyRemoteStorageMetadata(
                    remoteId = current?.remoteId,
                    remoteLastUpdated = current?.remoteLastUpdated,
                    synced = false
                )
        }
        val ids = setLogEntryDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map {
            if (it.second == -1L) it.first else it.first.copy(id = it.second)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insertFromLiveWorkoutCompletedSets(
        workoutLogEntryId: Long,
        workoutId: Long,
        excludeFromCopy: List<Long>
    ) {
        setLogEntryDao.insertFromLiveWorkoutCompletedSets(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = workoutId,
            excludeFromCopy = excludeFromCopy,
        )

        syncScheduler.scheduleSync()
    }

    override suspend fun insert(model: SetLogEntry): Long {
        val insertId = setLogEntryDao.insert(model.toEntity())
        syncScheduler.scheduleSync()

        return insertId
    }

    override suspend fun insertMany(models: List<SetLogEntry>): List<Long> {
        val insertIds = setLogEntryDao.insertMany(models.fastMap { it.toEntity() })
        syncScheduler.scheduleSync()

        return insertIds
    }

    override suspend fun delete(model: SetLogEntry): Int =
        deleteById(model.id)

    override suspend fun deleteMany(models: List<SetLogEntry>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = setLogEntryDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val deleteCount = setLogEntryDao.softDelete(id)
        if (deleteCount > 0) {
            syncScheduler.scheduleSync()
        }
        return deleteCount
    }

    override suspend fun deleteByWorkoutLogEntryId(workoutLogEntryId: Long): Int {
        val deleteCount = setLogEntryDao.softDeleteByWorkoutLogEntryId(workoutLogEntryId)
        if (deleteCount > 0) {
            syncScheduler.scheduleSync()
        }
        return deleteCount
    }
}