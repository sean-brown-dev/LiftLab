package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart


class LiftMetricChartRepository(private val liftMetricChartsDao: LiftMetricChartsDao): Repository {
    suspend fun deleteAllWithNoLifts() {
        liftMetricChartsDao.deleteAllWithNoLift()
    }

    suspend fun upsertMany(liftMetricCharts: List<LiftMetricChartDto>) {
        val charts = liftMetricCharts.fastMap { liftMetricChart ->
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            )
        }

        liftMetricChartsDao.upsertMany(charts)
    }

    suspend fun upsert(liftMetricChart: LiftMetricChartDto) {
        liftMetricChartsDao.upsert(
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            )
        )
    }

    suspend fun getAll(): List<LiftMetricChartDto> {
        return liftMetricChartsDao.getAll().map {
            LiftMetricChartDto(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }
}