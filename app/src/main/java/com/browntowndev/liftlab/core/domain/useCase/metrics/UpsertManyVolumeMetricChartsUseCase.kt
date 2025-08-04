package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository

class UpsertManyVolumeMetricChartsUseCase(
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository
) {
    suspend operator fun invoke(volumeMetricCharts: List<VolumeMetricChart>) {
        volumeMetricChartsRepository.upsertMany(volumeMetricCharts)
    }
}