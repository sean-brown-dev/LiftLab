package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap

import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.mapping.SetLogEntryMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.SetLogEntryMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import kotlinx.coroutines.flow.Flow

class SetLogEntryRepositoryImpl(
    private val setLogEntryDao: SetLogEntryDao,
    private val syncScheduler: SyncScheduler,
): SetLogEntryRepository {

    override suspend fun insertFromPreviousSetResults(
        workoutLogEntryId: Long,
        workoutId: Long,
        mesocycle: Int,
        microcycle: Int,
        excludeFromCopy: List<Long>
    ) {
        setLogEntryDao.insertFromPreviousSetResults(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = workoutId,
            mesocycle = mesocycle,
            microcycle = microcycle,
            excludeFromCopy = excludeFromCopy,
        )

        syncScheduler.scheduleSync()
    }

    override suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecord> {
        return setLogEntryDao.getPersonalRecordsForLifts(liftIds).fastMap {
            PersonalRecord(
                liftId = it.liftId,
                personalRecord = it.personalRecord,
            )
        }
    }

    override suspend fun getAll(): List<SetLogEntry> =
        setLogEntryDao.getAll().fastMap { it.toDomainModel() }

    override fun getAllFlow(): Flow<List<SetLogEntry>> {
        TODO("Not yet implemented")
    }

    override suspend fun getById(id: Long): SetLogEntry? =
        setLogEntryDao.get(id)?.toDomainModel()

    override suspend fun getMany(ids: List<Long>): List<SetLogEntry> =
        setLogEntryDao.getMany(ids).fastMap { it.toDomainModel() }

    override suspend fun update(model: SetLogEntry) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMany(models: List<SetLogEntry>) {
        TODO("Not yet implemented")
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

    override suspend fun insert(model: SetLogEntry): Long {
        TODO("Not yet implemented")
    }

    override suspend fun insertMany(models: List<SetLogEntry>): List<Long> {
        TODO("Not yet implemented")
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
}