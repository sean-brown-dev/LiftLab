package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

class GetGroupedLiftMetricChartDataUseCase {
    operator fun invoke(
        liftMetricCharts: List<LiftMetricChart>,
        workoutLogs: List<WorkoutLogEntry>,
    ): Map<Long, List<WorkoutLogEntry>> {
        if (liftMetricCharts.isEmpty() || workoutLogs.isEmpty()) return emptyMap()

        val liftIdsWithCharts = liftMetricCharts.mapNotNull { it.liftId }
        return liftIdsWithCharts.associateWith { liftId ->
            workoutLogs.mapNotNull { workoutLog ->
                val setsForLift = workoutLog.setLogEntries.filter { it.liftId == liftId }
                if (setsForLift.isNotEmpty()) {
                    workoutLog.copy(setLogEntries = setsForLift)
                } else {
                    null
                }
            }.sortedBy { it.date }
        }
    }
}