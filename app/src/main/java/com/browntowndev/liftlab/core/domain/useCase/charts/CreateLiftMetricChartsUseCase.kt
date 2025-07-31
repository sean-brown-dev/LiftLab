package com.browntowndev.liftlab.core.domain.useCase.charts

import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository

class CreateLiftMetricChartsUseCase(
    private val liftMetricChartsRepository: LiftMetricChartsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(liftIds: List<Long>) = transactionScope.execute {
        var liftMetricCharts = liftMetricChartsRepository.getMany(liftIds)
        val firstLiftId = liftIds.first()

        // One chart template exists with only the chart type assigned,
        // so the first lift can just fill in the template chart's liftId
        // but the remaining need to make new charts
        liftMetricCharts = liftIds.fastFlatMap { currLiftId ->
            liftMetricCharts.fastMap { chart ->
                updateChart(chart, currLiftId, firstLiftId)
            }
        }

        liftMetricChartsRepository.upsertMany(liftMetricCharts)
    }

    private fun updateChart(chart: LiftMetricChart, liftId: Long, firstLiftId: Long): LiftMetricChart {
        return chart.copy(
            id = if (liftId == firstLiftId) chart.id else 0L,
            liftId = liftId
        )
    }
}