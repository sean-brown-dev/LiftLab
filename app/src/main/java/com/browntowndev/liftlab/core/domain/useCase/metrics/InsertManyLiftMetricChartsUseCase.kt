package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository

class InsertManyLiftMetricChartsUseCase(
    private val liftMetricChartsRepository: LiftMetricChartsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(charts: List<LiftMetricChart>): List<Long> =  transactionScope.execute {
        // Clear out table of charts with no lifts in case any get stranded somehow
        liftMetricChartsRepository.deleteAllWithNoLifts()
        return@execute liftMetricChartsRepository.insertMany(charts)
    }
}