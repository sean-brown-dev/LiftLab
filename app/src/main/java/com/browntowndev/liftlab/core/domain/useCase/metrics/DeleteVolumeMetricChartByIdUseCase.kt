package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository

class DeleteVolumeMetricChartByIdUseCase(
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(id: Long) = transactionScope.execute {
        volumeMetricChartsRepository.deleteById(id)
    }
}