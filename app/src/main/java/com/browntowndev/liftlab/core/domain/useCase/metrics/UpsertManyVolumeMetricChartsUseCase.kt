package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository

class UpsertManyVolumeMetricChartsUseCase(
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(volumeMetricCharts: List<VolumeMetricChart>) = transactionScope.execute {
        volumeMetricChartsRepository.upsertMany(volumeMetricCharts)
    }
}