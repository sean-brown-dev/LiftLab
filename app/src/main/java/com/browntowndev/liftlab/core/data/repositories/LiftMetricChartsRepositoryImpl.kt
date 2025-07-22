package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.LiftMetricChart
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.data.local.entities.LiftMetricChartEntity
import com.browntowndev.liftlab.core.data.local.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class LiftMetricChartsRepositoryImpl(
    private val liftMetricChartsDao: LiftMetricChartsDao,
    private val syncScheduler: SyncScheduler,
) : LiftMetricChartsRepository {
    override suspend fun deleteAllWithNoLifts() {
        val toDelete = liftMetricChartsDao.getAllWithNoLift()
        if (toDelete.isEmpty()) return

        val count = liftMetricChartsDao.softDeleteMany(toDelete.map { it.id })
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
    }

    override suspend fun upsert(model: LiftMetricChart): Long {
        val current = liftMetricChartsDao.get(model.id)
        val toUpsert =
            LiftMetricChartEntity(
                id = model.id,
                liftId = model.liftId,
                chartType = model.chartType,
            ).applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.lastUpdated,
                synced = false,
            )

        val upsertId = liftMetricChartsDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        syncScheduler.scheduleSync()

        return if (upsertId == -1L) toUpsert.id else upsertId
    }

    override suspend fun upsertMany(models: List<LiftMetricChart>): List<Long> {
        val currentCharts = liftMetricChartsDao.getMany(models.fastMap { it.id })
            .associateBy { it.id }

        val charts = models.fastMap { liftMetricChart ->
            val current = currentCharts[liftMetricChart.id]
            LiftMetricChartEntity(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            ).applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.lastUpdated,
                synced = false,
            )
        }

        val upsertIds = liftMetricChartsDao.upsertMany(charts)
        val entityIds = charts.zip(upsertIds).map { (chart, id) ->
            if (id == -1L) chart else chart.copy(id = id)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun getAll(): List<LiftMetricChart> {
        return liftMetricChartsDao.getAllForExistingLifts().fastMap {
            LiftMetricChart(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    override fun getAllFlow(): Flow<List<LiftMetricChart>> {
        return liftMetricChartsDao.getAllFlow().map {
            it.fastMap { entity ->
                LiftMetricChart(
                    id = entity.id,
                    liftId = entity.liftId,
                    chartType = entity.chartType,
                )
            }
        }
    }


    override suspend fun getMany(ids: List<Long>): List<LiftMetricChart> {
        return liftMetricChartsDao.getMany(ids).fastMap {
            LiftMetricChart(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    override suspend fun getById(id: Long): LiftMetricChart? {
        return liftMetricChartsDao.get(id)?.let {
            LiftMetricChart(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    override suspend fun update(model: LiftMetricChart) {
        val entity = LiftMetricChartEntity(
            id = model.id,
            liftId = model.liftId,
            chartType = model.chartType
        )
        liftMetricChartsDao.update(entity)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<LiftMetricChart>) {
        val entities = models.map {
            LiftMetricChartEntity(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType
            )
        }
        liftMetricChartsDao.updateMany(entities)
        syncScheduler.scheduleSync()
    }

    override suspend fun insert(model: LiftMetricChart): Long {
        val entity = LiftMetricChartEntity(
            id = model.id,
            liftId = model.liftId,
            chartType = model.chartType
        )
        val newId = liftMetricChartsDao.insert(entity)
        syncScheduler.scheduleSync()

        return newId
    }

    override suspend fun insertMany(models: List<LiftMetricChart>): List<Long> {
        val entities = models.map {
            LiftMetricChartEntity(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType
            )
        }
        val newIds = liftMetricChartsDao.insertMany(entities)
        syncScheduler.scheduleSync()

        return newIds
    }

    override suspend fun delete(model: LiftMetricChart): Int {
        val count = liftMetricChartsDao.softDelete(model.id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<LiftMetricChart>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = liftMetricChartsDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val count = liftMetricChartsDao.softDelete(id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }
}