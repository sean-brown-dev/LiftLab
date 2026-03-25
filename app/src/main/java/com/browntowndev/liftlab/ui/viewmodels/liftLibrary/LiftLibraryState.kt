package com.browntowndev.liftlab.ui.viewmodels.liftLibrary

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption
import com.browntowndev.liftlab.ui.models.workout.LiftUiModel

@Stable
data class LiftLibraryState (
    val allLifts: List<LiftUiModel> = listOf(),
    val filteredLifts: List<LiftUiModel> = listOf(),
    val selectedLifts: List<Long> = listOf(),
    val addAtPosition: Int? = null,
    val workoutId: Long? = null,
    val nameFilter: String? = null,
    val newLiftMetricChartIds: List<Long> = listOf(),
    val liftsToFilterOut: HashSet<Long> = hashSetOf(),
    val movementPatternFilters: List<FilterChipOption> = listOf(),
    val showFilterSelection: Boolean = false,
    val replacingLift: Boolean = false,
    val confirmMergeDialogVisible: Boolean = false,
    val mergeLiftName: String = ""
) {
    val selectedLiftNames by lazy {
        allLifts.fastMapNotNull {
            if (it.id in selectedLiftsSet) it.name else null
        }
    }

    val selectedLiftsSet by lazy {
        selectedLifts.toHashSet()
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