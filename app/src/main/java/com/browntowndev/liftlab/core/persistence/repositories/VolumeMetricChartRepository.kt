package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.VolumeMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart

class VolumeMetricChartRepository(private val volumeMetricChartsDao: VolumeMetricChartsDao) {
    suspend fun upsert(volumeMetricChart: VolumeMetricChartDto): Long {
        return volumeMetricChartsDao.upsert(
            VolumeMetricChart(
                id = volumeMetricChart.id,
                volumeType = volumeMetricChart.volumeType,
                volumeTypeImpact = volumeMetricChart.volumeTypeImpact,
            )
        )
    }

    suspend fun upsertMany(volumeMetricCharts: List<VolumeMetricChartDto>): List<Long> {
        return volumeMetricChartsDao.upsertMany(
            volumeMetricCharts.fastMap { volumeMetricChart ->
                VolumeMetricChart(
                    id = volumeMetricChart.id,
                    volumeType = volumeMetricChart.volumeType,
                    volumeTypeImpact = volumeMetricChart.volumeTypeImpact,
                )
            }
        )
    }

    suspend fun getAll(): List<VolumeMetricChartDto> {
        return volumeMetricChartsDao.getAll().fastMap {
            VolumeMetricChartDto(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    suspend fun delete(id: Long) {
        volumeMetricChartsDao.delete(id)
    }
}