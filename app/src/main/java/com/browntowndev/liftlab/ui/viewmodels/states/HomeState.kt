package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricChartModel

data class HomeState(
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val workoutCompletionChart: ChartModel? = null,
    val microCycleCompletionChart: ChartModel? = null,
    val showLiftChartPicker: Boolean = false,
    val liftChartTypeSelections: List<String> = listOf(),
    val liftMetricChartModels: List<LiftMetricChartModel> = listOf(),
)
