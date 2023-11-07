package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto

data class WorkoutHistoryState(
    val dateOrderedWorkoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val topSets: Map<Long, Map<Long, Pair<Int, SetLogEntryDto>>> = mapOf(),
)
