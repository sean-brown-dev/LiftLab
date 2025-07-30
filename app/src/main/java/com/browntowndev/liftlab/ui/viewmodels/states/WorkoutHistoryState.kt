package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.FlowRowFilterChipSection
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

data class WorkoutHistoryState(
    val filterChips: List<FilterChipOption> = listOf(),
    val dateOrderedWorkoutLogs: List<WorkoutLogEntry> = listOf(),
    val filteredWorkoutLogs: List<WorkoutLogEntry> = listOf(),
    val topSets: Map<Long, Map<Long, Pair<Int, SetLogEntry>>> = mapOf(),
    val isDatePickerVisible: Boolean = false,
    val isProgramAndWorkoutFilterVisible: Boolean = false,
    val startDateInMillis: Long? = null,
    val endDateInMillis: Long? = null,
    val programAndWorkoutFilters: List<FilterChipOption> = listOf(),
    val workoutIdFilters: List<FilterChipOption> = listOf(),
    val workoutNamesById: Map<Long, String> = mapOf(),
    val programNamesById: Map<Long, String> = mapOf(),
    val programAndWorkoutFilterSections: List<FlowRowFilterChipSection> = listOf(),
) {
    val dateRangeFilter by lazy {
        val endDateInclusive = endDateInMillis?.toDate()?.toLocalDate()?.plusDays(1)?.toEndOfDate()?.toInstant()?.toEpochMilli()
        (startDateInMillis ?: 0)..(endDateInclusive ?: Long.MAX_VALUE)
    }
}
