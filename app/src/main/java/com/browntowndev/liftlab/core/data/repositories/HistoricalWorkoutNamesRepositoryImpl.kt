package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.mapping.HistoricalWorkoutNameMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.HistoricalWorkoutNameMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.models.workoutLogging.HistoricalWorkoutName
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.data.local.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoricalWorkoutNamesRepositoryImpl(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
    private val syncScheduler: SyncScheduler,
) : HistoricalWorkoutNamesRepository {
    override suspend fun getAll(): List<HistoricalWorkoutName> {
        return historicalWorkoutNamesDao.getAll().map { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<HistoricalWorkoutName>> {
        return historicalWorkoutNamesDao.getAllFlow().map { it.map { entity -> entity.toDomainModel() } }
    }

    override suspend fun getById(id: Long): HistoricalWorkoutName? {
        return historicalWorkoutNamesDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutName> {
        return historicalWorkoutNamesDao.getMany(ids).map { it.toDomainModel() }
    }

    override suspend fun update(model: HistoricalWorkoutName) {
        val toUpdate = model.toEntity()
        historicalWorkoutNamesDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<HistoricalWorkoutName>) {
        val toUpdate = models.map { it.toEntity() }
        historicalWorkoutNamesDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: HistoricalWorkoutName): Long {
        val toUpsert = model.toEntity()
        val id = historicalWorkoutNamesDao.upsert(toUpsert)
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<HistoricalWorkoutName>): List<Long> {
        val toUpsert = models.map { it.toEntity() }
        val ids = historicalWorkoutNamesDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, returnedId) ->
            if (returnedId == -1L) entity else entity.copy(id = returnedId)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insert(model: HistoricalWorkoutName): Long {
        val toInsert = model.toEntity()
        val id = historicalWorkoutNamesDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<HistoricalWorkoutName>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val ids = historicalWorkoutNamesDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun delete(model: HistoricalWorkoutName): Int {
        val count = historicalWorkoutNamesDao.softDelete(model.id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<HistoricalWorkoutName>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = historicalWorkoutNamesDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val count = historicalWorkoutNamesDao.softDelete(id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long? {
        return historicalWorkoutNamesDao.getByProgramAndWorkoutId(programId, workoutId)?.id
    }
}