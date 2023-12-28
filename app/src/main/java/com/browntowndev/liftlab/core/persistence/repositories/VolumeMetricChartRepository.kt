package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.enums.getVolumeTypeImpacts
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartDao
import com.browntowndev.liftlab.core.persistence.dtos.VolumeMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart

class VolumeMetricChartRepository(private val volumeMetricChartDao: VolumeMetricChartDao) {
    fun upsert(volumeMetricChart: VolumeMetricChartDto): Long {
        return volumeMetricChartDao.upsert(
            VolumeMetricChart(
                id = volumeMetricChart.id,
                volumeType = volumeMetricChart.volumeType,
                volumeTypeImpactBitmask = volumeMetricChart.volumeTypeImpactsBitmask,
            )
        )
    }

    fun upsertMany(volumeMetricCharts: List<VolumeMetricChartDto>): List<Long> {
        return volumeMetricChartDao.upsertMany(
            volumeMetricCharts.fastMap { volumeMetricChart ->
                VolumeMetricChart(
                    id = volumeMetricChart.id,
                    volumeType = volumeMetricChart.volumeType,
                    volumeTypeImpactBitmask = volumeMetricChart.volumeTypeImpactsBitmask,
                )
            }
        )
    }

    fun getAll(): List<VolumeMetricChartDto> {
        return volumeMetricChartDao.getAll().fastMap {
            VolumeMetricChartDto(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpacts = it.volumeTypeImpactBitmask.getVolumeTypeImpacts(),
            )
        }
    }

    fun delete(id: Long) {
        volumeMetricChartDao.delete(id)
    }
}