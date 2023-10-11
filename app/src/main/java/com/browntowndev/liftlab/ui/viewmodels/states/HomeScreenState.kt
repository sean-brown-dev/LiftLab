package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel

data class HomeScreenState(
    val program: ProgramDto? = null,
    val dateOrderedWorkoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val topSets: Map<String, Pair<Int, SetLogEntryDto>> = mapOf(),
    val workoutCompletionChart: ChartModel? = null,
    val microCycleCompletionChart: ChartModel? = null,
)
