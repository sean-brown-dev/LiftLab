package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.dtos.LiftDto
import com.browntowndev.liftlab.core.data.repositories.LiftsRepository
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLibraryState
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLibraryScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LiftLibraryViewModel(private val liftsRepository: LiftsRepository): ViewModel() {
    private val allLifts: MutableList<LiftDto> = mutableListOf()
    private val _state = MutableStateFlow(LiftLibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getAllLifts()
        }
    }

    fun watchActionBarActions(screen: LiftLibraryScreen?, topAppBarViewModel: TopAppBarViewModel) {
        viewModelScope.launch {
            screen?.stringActionButtons?.collect {
                when(it.action) {
                    LiftLibraryScreen.Companion.AppBarPayloadActions.FilterTextChanged -> {
                        topAppBarViewModel.mutateControlValue(
                            AppBarMutateControlRequest(LiftLibraryScreen.LIFT_FILTER_TEXTVIEW, it.value))

                        filterLifts(it.value)
                    }
                }
            }
        }

        viewModelScope.launch {
            screen?.simpleActionButtons?.collect {
                when (it) {
                    LiftLibraryScreen.Companion.AppBarActions.SearchToggled,
                    LiftLibraryScreen.Companion.AppBarActions.NavigatedBack-> topAppBarViewModel.toggleControlVisibility(
                        LiftLibraryScreen.LIFT_FILTER_TEXTVIEW)
                }
            }
        }
    }

    private fun filterLifts(filter: String) {
        _state.update {
            it.copy(lifts = this.allLifts.filter { lift -> lift.name.contains(filter, true) }.toList())
        }
    }

    private suspend fun getAllLifts() {
        val lifts = liftsRepository.getAll().sortedBy { it.name }
        this.allLifts.addAll(lifts)

        _state.update {
            it.copy(lifts = lifts)
        }
    }
}