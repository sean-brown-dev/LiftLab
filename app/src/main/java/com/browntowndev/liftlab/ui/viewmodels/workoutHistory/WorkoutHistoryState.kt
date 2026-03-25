package com.browntowndev.liftlab.ui.viewmodels.workoutHistory

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption
import com.browntowndev.liftlab.ui.models.controls.FlowRowFilterChipSection
import com.browntowndev.liftlab.ui.models.metrics.AllWorkoutTopSetsUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.WorkoutLogEntryUiModel

@Stable
data class WorkoutHistoryState(
    val filterChips: List<FilterChipOption> = listOf(),
    val dateOrderedWorkoutLogs: List<WorkoutLogEntryUiModel> = listOf(),
    val filteredWorkoutLogs: List<WorkoutLogEntryUiModel> = listOf(),
    val topSets: AllWorkoutTopSetsUiModel = AllWorkoutTopSetsUiModel(mapOf()),
    val isDatePickerVisible: Boolean = false,
    val isProgramAndWorkoutFilterVisible: Boolean = false,
    val startDateInMillis: Long? = null,
    val endDateInMillis: Long? = null,
    val programAndWorkoutFilters: List<FilterChipOption> = listOf(),
    val workoutNamesById: Map<Long, String> = mapOf(),
    val programNamesById: Map<Long, String> = mapOf(),
    val programAndWorkoutFilterSections: List<FlowRowFilterChipSection> = listOf(),
) {
    val dateRangeFilter by lazy {
        val endDateInclusive = endDateInMillis?.toDate()?.toLocalDate()?.plusDays(1)?.toEndOfDate()?.toInstant()?.toEpochMilli()
        (startDateInMillis ?: 0)..(endDateInclusive ?: Long.MAX_VALUE)
    }

    val programAndWorkoutFiltersByKey by lazy {
        programAndWorkoutFilters.associateBy { it.key }
    }
}
