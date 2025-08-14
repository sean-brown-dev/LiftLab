package com.browntowndev.liftlab.core.data.local.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.data.local.entities.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VolumeMetricChartsRepositoryImpl(
    private val volumeMetricChartsDao: VolumeMetricChartsDao,
    private val syncScheduler: SyncScheduler,
) : VolumeMetricChartsRepository {
    override suspend fun upsert(model: VolumeMetricChart): Long {
        val current = volumeMetricChartsDao.get(model.id)
        val toUpsert = VolumeMetricChartEntity(
            id = model.id,
            volumeType = model.volumeType,
            volumeTypeImpact = model.volumeTypeImpact,
        ).applyRemoteStorageMetadata(
            remoteId = current?.remoteId,
            remoteLastUpdated = current?.remoteLastUpdated,
            synced = false
        )
        val id = volumeMetricChartsDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<VolumeMetricChart>): List<Long> {
        val currentEntities = volumeMetricChartsDao.getMany(models.map { it.id }).associateBy { it.id }
        val toUpsert =
            models.fastMap { volumeMetricChart ->
                val current = currentEntities[volumeMetricChart.id]
                VolumeMetricChartEntity(
                    id = volumeMetricChart.id,
                    volumeType = volumeMetricChart.volumeType,
                    volumeTypeImpact = volumeMetricChart.volumeTypeImpact,
                ).applyRemoteStorageMetadata(
                    remoteId = current?.remoteId,
                    remoteLastUpdated = current?.remoteLastUpdated,
                    synced = false
                )
            }
        val upsertIds = volumeMetricChartsDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(upsertIds).fastMap { (toUpsert, upsertId) ->
            if (upsertId == -1L) toUpsert else toUpsert.copy(id = upsertId)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insert(model: VolumeMetricChart): Long {
        val toInsert = VolumeMetricChartEntity(
            id = model.id,
            volumeType = model.volumeType,
            volumeTypeImpact = model.volumeTypeImpact,
        )
        val id = volumeMetricChartsDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<VolumeMetricChart>): List<Long> {
        val toInsert = models.map {
            VolumeMetricChartEntity(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
        val ids = volumeMetricChartsDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun delete(model: VolumeMetricChart): Int {
        val count = volumeMetricChartsDao.softDelete(model.id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<VolumeMetricChart>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = volumeMetricChartsDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun getAll(): List<VolumeMetricChart> {
        return volumeMetricChartsDao.getAll().fastMap {
            VolumeMetricChart(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    override fun getAllFlow(): Flow<List<VolumeMetricChart>> {
        return volumeMetricChartsDao.getAllFlow().map {
            it.fastMap { entity ->
                VolumeMetricChart(
                    id = entity.id,
                    volumeType = entity.volumeType,
                    volumeTypeImpact = entity.volumeTypeImpact,
                )
            }
        }
    }

    override suspend fun getById(id: Long): VolumeMetricChart? {
        return volumeMetricChartsDao.get(id)?.let {
            VolumeMetricChart(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    override suspend fun getMany(ids: List<Long>): List<VolumeMetricChart> {
        return volumeMetricChartsDao.getMany(ids).map {
            VolumeMetricChart(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    override suspend fun update(model: VolumeMetricChart) {
        val existing = volumeMetricChartsDao.get(model.id) ?: return
        val toUpdate = VolumeMetricChartEntity(
            id = model.id,
            volumeType = model.volumeType,
            volumeTypeImpact = model.volumeTypeImpact,
        ).applyRemoteStorageMetadata(
            remoteId = existing.remoteId,
            remoteLastUpdated = existing.remoteLastUpdated,
            synced = false
        )
        volumeMetricChartsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<VolumeMetricChart>) {
        val existingById = volumeMetricChartsDao.getMany(models.map { it.id }).associateBy { it.id }
        val toUpdate = models.map {
            VolumeMetricChartEntity(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            ).applyRemoteStorageMetadata(
                remoteId = existingById[it.id]?.remoteId,
                remoteLastUpdated = existingById[it.id]?.remoteLastUpdated,
                synced = false
            )
        }
        volumeMetricChartsDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun deleteById(id: Long): Int {
        val count = volumeMetricChartsDao.softDelete(id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }
}