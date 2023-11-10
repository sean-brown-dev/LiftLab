package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart


class LiftMetricChartRepository(private val liftMetricChartsDao: LiftMetricChartsDao): Repository {
    suspend fun deleteAllWithNoLifts() {
        liftMetricChartsDao.deleteAllWithNoLift()
    }

    suspend fun upsertMany(liftMetricCharts: List<LiftMetricChartDto>): List<Long> {
        val charts = liftMetricCharts.fastMap { liftMetricChart ->
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            )
        }

        return liftMetricChartsDao.upsertMany(charts)
    }

    suspend fun upsert(liftMetricChart: LiftMetricChartDto): Long {
        return liftMetricChartsDao.upsert(
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            )
        )
    }

    suspend fun getAll(): List<LiftMetricChartDto> {
        return liftMetricChartsDao.getAll().fastMap {
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