package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository

class DeleteLiftMetricChartByIdUseCase(
    private val liftMetricChartsRepository: LiftMetricChartsRepository,
) {
    suspend operator fun invoke(id: Long) {
        liftMetricChartsRepository.deleteById(id)
    }
}