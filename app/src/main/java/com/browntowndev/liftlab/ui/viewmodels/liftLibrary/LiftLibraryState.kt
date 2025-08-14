package com.browntowndev.liftlab.ui.viewmodels.liftLibrary

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption
import com.browntowndev.liftlab.ui.models.workout.LiftUiModel

@Stable
data class LiftLibraryState (
    val allLifts: List<LiftUiModel> = listOf(),
    val filteredLifts: List<LiftUiModel> = listOf(),
    val selectedNewLifts: List<Long> = listOf(),
    val addAtPosition: Int? = null,
    val workoutId: Long? = null,
    val nameFilter: String? = null,
    val newLiftMetricChartIds: List<Long> = listOf(),
    val liftsToFilterOut: HashSet<Long> = hashSetOf(),
    val movementPatternFilters: List<FilterChipOption> = listOf(),
    val showFilterSelection: Boolean = false,
    val replacingLift: Boolean = false,
) {
    val selectedNewLiftsHashSet by lazy {
        selectedNewLifts.toHashSet()
    }

    val allFilters by lazy {
        movementPatternFilters.map {
            it
        }.toMutableList().apply {
            if(nameFilter?.isNotEmpty() == true) {
                add(FilterChipOption(FilterChipOption.NAME, nameFilter))
            }
        }
    }
}