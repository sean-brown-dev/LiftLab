package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto

data class WorkoutHistoryState(
    val filterChips: List<FilterChipOption> = listOf(),
    val dateOrderedWorkoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val filteredWorkoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val topSets: Map<Long, Map<Long, Pair<Int, SetLogEntryDto>>> = mapOf(),
    val isDatePickerVisible: Boolean = false,
    val startDateInMillis: Long? = null,
    val endDateInMillis: Long? = null,
    val programIdFilters: List<Long> = listOf(),
    val workoutIdFilters: List<Long> = listOf(),
) {
    val dateRangeFilter by lazy {
        val endDateInclusive = endDateInMillis?.toDate()?.toLocalDate()?.plusDays(1)?.toEndOfDate()?.toInstant()?.toEpochMilli()
        (startDateInMillis ?: 0)..(endDateInclusive ?: Long.MAX_VALUE)
    }

    val workoutNamesById by lazy {
        dateOrderedWorkoutLogs
            .distinctBy { it.workoutId }
            .associate {
                it.workoutId to it.workoutName
            }
    }

    val programNamesById by lazy {
        dateOrderedWorkoutLogs
            .distinctBy { it.programId }
            .associate {
                it.programId to it.programName
            }
    }
}
