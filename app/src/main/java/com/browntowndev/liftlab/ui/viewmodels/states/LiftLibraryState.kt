package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto

@Stable
data class LiftLibraryState (
    val allLifts: List<LiftDto> = listOf(),
    val filteredLifts: List<LiftDto> = listOf(),
    val selectedNewLifts: List<Long> = listOf(),
    val addAtPosition: Int? = null,
    val workoutId: Long? = null,
    val nameFilter: String? = null,
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