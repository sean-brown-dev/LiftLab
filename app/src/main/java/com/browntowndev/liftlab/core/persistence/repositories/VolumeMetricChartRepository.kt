package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.VolumeMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata

class VolumeMetricChartRepository(private val volumeMetricChartsDao: VolumeMetricChartsDao) {
    suspend fun upsert(volumeMetricChart: VolumeMetricChartDto): Long {
        val current = volumeMetricChartsDao.get(volumeMetricChart.id)
        return volumeMetricChartsDao.upsert(
            VolumeMetricChart(
                id = volumeMetricChart.id,
                volumeType = volumeMetricChart.volumeType,
                volumeTypeImpact = volumeMetricChart.volumeTypeImpact,
            ).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false
            )
        )
    }

    suspend fun upsertMany(volumeMetricCharts: List<VolumeMetricChartDto>): List<Long> {
        val currentEntities = volumeMetricChartsDao.getMany(volumeMetricCharts.map { it.id }).associateBy { it.id }
        return volumeMetricChartsDao.upsertMany(
            volumeMetricCharts.fastMap { volumeMetricChart ->
                val current = currentEntities[volumeMetricChart.id]
                VolumeMetricChart(
                    id = volumeMetricChart.id,
                    volumeType = volumeMetricChart.volumeType,
                    volumeTypeImpact = volumeMetricChart.volumeTypeImpact,
                ).copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
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