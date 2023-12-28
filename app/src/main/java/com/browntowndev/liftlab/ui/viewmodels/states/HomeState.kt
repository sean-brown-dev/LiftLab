package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricOptions

data class HomeState(
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val workoutCompletionChart: ChartModel? = null,
    val microCycleCompletionChart: ChartModel? = null,
    val showLiftChartPicker: Boolean = false,
    val volumeTypeSelections: List<String> = listOf(),
    val volumeImpactSelections: List<String> = listOf(),
    val liftChartTypeSelections: List<String> = listOf(),
    val liftMetricOptions: List<LiftMetricOptions> = listOf(),
    val liftMetricChartModels: List<LiftMetricChartModel> = listOf(),
)
