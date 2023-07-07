package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLibraryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LiftLibraryViewModel(
    private val liftsRepository: LiftsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val transactionScope: TransactionScope,
    private val eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private val _state = MutableStateFlow(LiftLibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getAllLifts()
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(event: TopAppBarEvent.ActionEvent) {
        when (event.action) {
            TopAppBarAction.FilterStarted -> toggleFilterSelection()
            TopAppBarAction.NavigatedBack -> if (_state.value.showFilterSelection) setNavigateBackIconClickedState(true)
            else -> {}
        }
    }

    @Subscribe
    fun handleTopAppBarPayloadEvent(payloadEvent: TopAppBarEvent.PayloadActionEvent<String>) {
        when (payloadEvent.action) {
            TopAppBarAction.SearchTextChanged -> filterLiftsByName(payloadEvent.payload)
            else -> {}
        }
    }

    fun addWorkoutLift(workoutId: Long, position: Int, newLiftId: Long) {
        viewModelScope.launch {
            val newLift = _state.value.filteredLifts.find { it.id == newLiftId }!!
            val newWorkoutLift = StandardWorkoutLiftDto(
                liftId = newLift.id,
                workoutId = workoutId,
                liftName = newLift.name,
                liftMovementPattern = newLift.movementPattern,
                liftIncrementOverride = newLift.incrementOverride,
                liftRestTime = newLift.restTime,
                liftVolumeTypes = newLift.volumeTypesBitmask,
                position = position,
                deloadWeek = null,
                setCount = 3,
                incrementOverride = newLift.incrementOverride,
                restTime = newLift.restTime,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            )
            workoutLiftsRepository.insert(newWorkoutLift)
        }
    }

    fun replaceWorkoutLift(
        workoutLiftId: Long,
        replacementLiftId: Long,
    ) {
        viewModelScope.launch {
            workoutLiftsRepository.updateLiftId(workoutLiftId = workoutLiftId, newLiftId = replacementLiftId)
        }
    }

    private fun setNavigateBackIconClickedState(clicked: Boolean) {
        _state.update {
            it.copy(backNavigationClicked = clicked)
        }
    }

    private fun toggleFilterSelection() {
        _state.update {
            it.copy(showFilterSelection = !_state.value.showFilterSelection)
        }
    }

    private fun filterLiftsByName(filter: String) {
        _state.update {
            it.copy(nameFilter = filter)
        }
    }

    fun filterLiftsByMovementPatterns(movementPatterns: List<String>) {
        _state.update {
            it.copy(movementPatternFilters = movementPatterns, showFilterSelection = false, backNavigationClicked = false)
        }
    }

    fun removeMovementPatternFilter(movementPattern: String) {
        this.filterLiftsByMovementPatterns(_state.value.movementPatternFilters.filter { it != movementPattern })
    }

    private suspend fun getAllLifts() {
        val lifts = liftsRepository.getAll().sortedBy { it.name }

        _state.update {
            it.copy(allLifts = lifts)
        }
    }
}