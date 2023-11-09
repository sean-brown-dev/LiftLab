package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel

data class HomeScreenState(
    val dateOrderedWorkoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val workoutCompletionChart: ChartModel? = null,
    val microCycleCompletionChart: ChartModel? = null,
    val showLiftChartPicker: Boolean = false,
    val liftChartTypeSelections: List<String> = listOf(),
)
