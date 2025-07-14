package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata


class LiftMetricChartRepository(private val liftMetricChartsDao: LiftMetricChartsDao): Repository {
    suspend fun deleteAllWithNoLifts() {
        liftMetricChartsDao.deleteAllWithNoLift()
    }

    suspend fun delete(id: Long) {
        liftMetricChartsDao.delete(id)
    }

    suspend fun upsertMany(liftMetricCharts: List<LiftMetricChartDto>): List<Long> {
        val currentCharts = liftMetricChartsDao.getMany(liftMetricCharts.fastMap { it.id })
            .associateBy { it.id }

        val charts = liftMetricCharts.fastMap { liftMetricChart ->
            val current = currentCharts[liftMetricChart.id]
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            ).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        }

        return liftMetricChartsDao.upsertMany(charts)
    }

    suspend fun upsert(liftMetricChart: LiftMetricChartDto): Long {
        val current = liftMetricChartsDao.get(liftMetricChart.id)
        return liftMetricChartsDao.upsert(
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            ).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        )
    }

    suspend fun getAll(): List<LiftMetricChartDto> {
        return liftMetricChartsDao.getAllForExistingLifts().fastMap {
            LiftMetricChartDto(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    suspend fun getMany(ids: List<Long>): List<LiftMetricChartDto> {
        return liftMetricChartsDao.getMany(ids).fastMap {
            LiftMetricChartDto(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }
}