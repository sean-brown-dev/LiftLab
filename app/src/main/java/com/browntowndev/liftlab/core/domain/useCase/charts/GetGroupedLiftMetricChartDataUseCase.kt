package com.browntowndev.liftlab.core.domain.useCase.charts

import com.browntowndev.liftlab.core.domain.models.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry

class GetGroupedLiftMetricChartDataUseCase {
    operator fun invoke(
        liftMetricCharts: List<LiftMetricChart>,
        workoutLogs: List<WorkoutLogEntry>,
    ): Map<Long, List<WorkoutLogEntry>> {
        if (liftMetricCharts.isEmpty() || workoutLogs.isEmpty()) return emptyMap()

        val liftIdsWithCharts = liftMetricCharts.mapNotNull { it.liftId }
        return liftIdsWithCharts.associateWith { liftId ->
            workoutLogs.mapNotNull { workoutLog ->
                val setsForLift = workoutLog.setResults.filter { it.liftId == liftId }
                if (setsForLift.isNotEmpty()) {
                    workoutLog.copy(setResults = setsForLift)
                } else {
                    null
                }
            }.sortedBy { it.date }
        }
    }
}