package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository

class DeleteVolumeMetricChartByIdUseCase(
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository,
) {
    suspend operator fun invoke(id: Long) {
        volumeMetricChartsRepository.deleteById(id)
    }
}