package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto

@Stable
data class LiftLibraryState (
    val allLifts: List<LiftDto> = listOf(),
    val nameFilter: String? = null,
    val movementPatternFilters: List<String> = listOf(),
    val showFilterSelection: Boolean = false,
    val backNavigationClicked: Boolean = false,
) {
    val allFilters by lazy {
        val mutableMovementFilters = movementPatternFilters.map {
            FilterChipOption(FilterChipOption.MOVEMENT_PATTERN, it)
        }.toMutableList()

        if(nameFilter?.isNotEmpty() == true) {
            mutableMovementFilters.add(FilterChipOption(FilterChipOption.NAME, nameFilter))
        }

        mutableMovementFilters.toList()
    }
    val filteredLifts by lazy {
        val nameFilteredLifts = if (nameFilter?.isNotEmpty() == true) {
            allLifts.filter { lift -> lift.name.contains(nameFilter, true) }
        } else allLifts

        if (movementPatternFilters.isNotEmpty()) {
            nameFilteredLifts.filter { lift -> movementPatternFilters.contains(lift.movementPatternDisplayName) }
        } else nameFilteredLifts
    }
}