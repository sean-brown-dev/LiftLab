package com.browntowndev.liftlab.core.domain.models.metrics

import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

data class ConfiguredMetricsState(
    val activeProgram: Program? = null,
    val workoutLogs: List<WorkoutLogEntry> = emptyList(),
    val lifts: List<Lift> = emptyList(),
    val liftMetricCharts: List<LiftMetricChart> = emptyList(),
    val volumeMetricCharts: List<VolumeMetricChart> = emptyList(),
    val liftMetricChartData:  Map<Long, List<WorkoutLogEntry>> = emptyMap(),
    val volumeMetricChartData:  Map<VolumeMetricChart, List<WorkoutLogEntry>> = emptyMap(),
)
