package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLibraryState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LiftLibraryViewModel(
    private val liftsRepository: LiftsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val navHostController: NavHostController,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private val _state = MutableStateFlow(LiftLibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            liftsRepository.getAll()
                .observeForever { lifts ->
                    _state.update { currentState ->
                        currentState.copy(allLifts = lifts.sortedBy { it.name })
                    }
                }
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(event: TopAppBarEvent.ActionEvent) {
        when (event.action) {
            TopAppBarAction.FilterStarted -> toggleFilterSelection()
            TopAppBarAction.NavigatedBack -> if (_state.value.showFilterSelection) setNavigateBackIconClickedState(true)
            TopAppBarAction.ConfirmAddLift -> addWorkoutLifts()
            TopAppBarAction.CreateNewLift -> navigateToCreateLiftMenu()
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

    fun setWorkoutId(workoutId: Long?) {
        _state.update {
            it.copy(workoutId = workoutId)
        }
    }

    fun setAddAtPosition(position: Int?) {
        _state.update {
            it.copy(addAtPosition = position)
        }
    }

    fun addSelectedLift(id: Long) {
        _state.update {
            it.copy(selectedNewLifts = it.selectedNewLifts.toMutableList().apply {
                add(id)
            })
        }
    }

    fun removeSelectedLift(id: Long) {
        _state.update {
            it.copy(selectedNewLifts = it.selectedNewLifts.toMutableList().apply {
                remove(id)
            })
        }
    }

    private fun addWorkoutLifts() {
        viewModelScope.launch {
            // TODO: Block duplicate lift from being added
            val newLiftHashSet = _state.value.selectedNewLiftsHashSet
            val workoutId = _state.value.workoutId!!
            var position = _state.value.addAtPosition!! - 1
            val newLifts = _state.value.filteredLifts
                .filter { newLiftHashSet.contains(it.id) }
                .fastMap { newLift ->
                    position++
                    StandardWorkoutLiftDto(
                        liftId = newLift.id,
                        workoutId = workoutId,
                        liftName = newLift.name,
                        liftMovementPattern = newLift.movementPattern,
                        liftIncrementOverride = newLift.incrementOverride,
                        liftRestTime = newLift.restTime,
                        liftVolumeTypes = newLift.volumeTypesBitmask,
                        liftSecondaryVolumeTypes = newLift.secondaryVolumeTypesBitmask,
                        position = position,
                        deloadWeek = null,
                        setCount = 3,
                        incrementOverride = newLift.incrementOverride,
                        restTime = newLift.restTime,
                        rpeTarget = 8f,
                        repRangeBottom = 8,
                        repRangeTop = 10,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION_TOP_SET_RPE,
                    )
                }

            workoutLiftsRepository.insertAll(newLifts)
            navigateBackToWorkoutBuilder()
        }
    }

    fun replaceWorkoutLift(
        workoutLiftId: Long,
        replacementLiftId: Long,
    ) {
        viewModelScope.launch {
            workoutLiftsRepository.updateLiftId(workoutLiftId = workoutLiftId, newLiftId = replacementLiftId)
            navigateBackToWorkoutBuilder()
        }
    }

    private fun navigateBackToWorkoutBuilder() {
        val workoutBuilderRoute = WorkoutBuilderScreen.navigation.route.replace("{id}", _state.value.workoutId.toString())
        navHostController.popBackStack()
        navHostController.popBackStack()
        navHostController.navigate(workoutBuilderRoute)
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

    fun hideLift(lift: LiftDto) {
        viewModelScope.launch {
            // No need to update state. The lifts are retrieved via Flow
            liftsRepository.update(lift.copy(isHidden = true))
        }
    }

    private fun navigateToCreateLiftMenu() {
        navHostController.navigate(LiftDetailsScreen.navigation.route)
    }
}