package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository

class DeleteLiftMetricChartByIdUseCase(
    private val liftMetricChartsRepository: LiftMetricChartsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(id: Long) = transactionScope.execute {
        liftMetricChartsRepository.deleteById(id)
    }
}