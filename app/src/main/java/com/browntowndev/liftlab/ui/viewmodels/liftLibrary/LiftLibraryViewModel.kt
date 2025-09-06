package com.browntowndev.liftlab.ui.viewmodels.liftLibrary

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.DeleteLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.GetFilterableLiftsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.MergeLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.CreateLiftMetricChartsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.CreateWorkoutLiftsFromLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReplaceWorkoutLiftUseCase
import com.browntowndev.liftlab.ui.mapping.toDomainModel
import com.browntowndev.liftlab.ui.mapping.toUiModel
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.MOVEMENT_PATTERN
import com.browntowndev.liftlab.ui.models.controls.Route
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workout.LiftUiModel
import com.browntowndev.liftlab.ui.viewmodels.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LiftLibraryViewModel(
    private val deleteLiftUseCase: DeleteLiftUseCase,
    private val replaceWorkoutLiftUseCase: ReplaceWorkoutLiftUseCase,
    private val createLiftMetricChartsUseCase: CreateLiftMetricChartsUseCase,
    private val createWorkoutLiftsFromLiftsUseCase: CreateWorkoutLiftsFromLiftsUseCase,
    private val mergeLiftsUseCase: MergeLiftsUseCase,
    private val onNavigateHome: () -> Unit,
    private val onNavigateToWorkoutBuilder: (workoutId: Long) -> Unit,
    private val onNavigateToActiveWorkout: () -> Unit,
    private val onNavigateToLiftDetails: (liftId: Long?) -> Unit,
    getFilterableLiftsStateFlowUseCase: GetFilterableLiftsStateFlowUseCase,
    workoutId: Long?,
    private val mergeLiftId: Long?,
    addAtPosition: Int?,
    initialMovementPatternFilter: String,
    newLiftMetricChartIds: List<Long>,
    eventBus: EventBus,
): BaseViewModel(eventBus) {
    private val _state = MutableStateFlow(LiftLibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Setup state with initial movement pattern filter
            _state.update {
                it.copy(
                    movementPatternFilters = if (initialMovementPatternFilter.isNotEmpty()) {
                        listOf(
                            FilterChipOption(
                                type = MOVEMENT_PATTERN,
                                value = initialMovementPatternFilter
                            )
                        )
                    } else {
                        it.movementPatternFilters
                    }
                )
            }

            getFilterableLiftsStateFlowUseCase(workoutId)
                .map { liftConfigurationState ->
                    val sortedLifts = liftConfigurationState.lifts.sortedBy { it.name }
                    LiftLibraryState(
                        allLifts = sortedLifts.fastMap { it.toUiModel() },
                        liftsToFilterOut = liftConfigurationState.liftIdsForWorkout,
                    )
                }.onEach { state ->
                    _state.update {
                        it.copy(
                            allLifts = state.allLifts,
                            filteredLifts = getFilteredLifts(
                                liftsToFilter = state.allLifts,
                                nameFilter = _state.value.nameFilter,
                                movementPatternFilters = _state.value.movementPatternFilters,
                                liftIdFilters = state.liftsToFilterOut),
                            liftsToFilterOut = state.liftsToFilterOut,
                            workoutId = workoutId,
                            addAtPosition = addAtPosition,
                            newLiftMetricChartIds = newLiftMetricChartIds,
                        )
                    }
                }.launchIn(this)
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(event: TopAppBarEvent.ActionEvent) {
        when (event.action) {
            TopAppBarAction.FilterStarted -> toggleFilterSelection()
            TopAppBarAction.NavigatedBack -> if (_state.value.showFilterSelection) applyFilters()
            TopAppBarAction.Confirm -> onConfirmationClicked()
            TopAppBarAction.CreateNewLift -> onNavigateToLiftDetails(null)
            else -> {}
        }
    }

    @Subscribe
    fun handleTopAppBarPayloadEvent(payloadEvent: TopAppBarEvent.PayloadActionEvent<String>) {
        when (payloadEvent.action) {
            TopAppBarAction.SearchTextChanged -> setNameFilter(payloadEvent.payload)
            else -> {}
        }
    }

    private fun onConfirmationClicked() = executeWithErrorHandling("Failed to confirm") {
        when {
            _state.value.newLiftMetricChartIds.isNotEmpty() -> updateLiftMetricChartsWithSelectedLiftIds()
            mergeLiftId != null -> confirmMerge()
            else -> addWorkoutLifts()
        }
    }

    private suspend fun confirmMerge() {
        mergeLiftsUseCase(mergeLiftId!!, _state.value.selectedNewLifts)
    }

    fun addSelectedLift(id: Long) = executeWithErrorHandling("Failed to select lift") {
        _state.update {
            it.copy(selectedNewLifts = it.selectedNewLifts.toMutableList().apply {
                add(id)
            })
        }
    }

    fun removeSelectedLift(id: Long) = executeWithErrorHandling("Failed to deselect lift") {
        _state.update {
            it.copy(selectedNewLifts = it.selectedNewLifts.toMutableList().apply {
                remove(id)
            })
        }
    }

    private fun updateLiftMetricChartsWithSelectedLiftIds() = executeWithErrorHandling("Failed to create lift metric chart(s)") {
        val newLiftIds = _state.value.selectedNewLiftsHashSet
        createLiftMetricChartsUseCase(
            chartIds = _state.value.newLiftMetricChartIds,
            liftIds = newLiftIds.toList()
        )
        onNavigateHome()
    }

    private fun addWorkoutLifts() = executeWithErrorHandling("Failed to add lift(s)") {
        val newLiftHashSet = _state.value.selectedNewLiftsHashSet
        val newLifts = _state.value.filteredLifts.filter { it.id in newLiftHashSet }
        createWorkoutLiftsFromLiftsUseCase(
            workoutId = _state.value.workoutId!!,
            firstPosition = _state.value.addAtPosition!!,
            lifts = newLifts.fastMap { it.toDomainModel() }
        )
        navigateBackToWorkoutBuilder()
    }

    fun replaceWorkoutLift(
        workoutLiftId: Long,
        replacementLiftId: Long,
        callerRouteId: Long,
    ) = executeWithErrorHandling("Failed to replace lift") {
        _state.update { it.copy(replacingLift = true) }

        replaceWorkoutLiftUseCase(
            workoutId = _state.value.workoutId!!,
            workoutLiftId = workoutLiftId,
            replacementLiftId = replacementLiftId
        )
        if (callerRouteId == Route.WorkoutBuilder.id) {
            navigateBackToWorkoutBuilder()
        } else {
            navigateBackToActiveWorkout()
        }
    }

    private fun navigateBackToWorkoutBuilder() = executeWithErrorHandling("Failed to navigate back to workout builder") {
        onNavigateToWorkoutBuilder(_state.value.workoutId!!)
    }

    private fun navigateBackToActiveWorkout() = executeWithErrorHandling("Failed to navigate back to active workout") {
        onNavigateToActiveWorkout()
    }

    private fun toggleFilterSelection() = executeWithErrorHandling("Failed to toggle filter selection") {
        _state.update {
            it.copy(
                showFilterSelection = !_state.value.showFilterSelection
            )
        }
    }

    private fun setNameFilter(filter: String) = executeWithErrorHandling("Failed to set name filter") {
        _state.update {
            it.copy(nameFilter = filter)
        }

        applyFilters()
    }

    fun addMovementPatternFilter(movementPattern: FilterChipOption) = executeWithErrorHandling("Failed to add filter") {
        _state.update {
            it.copy(
                movementPatternFilters = it.movementPatternFilters
                    .toMutableList()
                    .apply { add(movementPattern) }
            )
        }
    }

    fun removeMovementPatternFilter(movementPattern: FilterChipOption, apply: Boolean) = executeWithErrorHandling("Failed to remove filter") {
        _state.update {
            it.copy(
                movementPatternFilters = _state.value.movementPatternFilters
                    .toMutableList()
                    .apply { remove(movementPattern) }
            )
        }

        if (apply) {
            applyFilters()
        }
    }

    private fun getFilteredLifts(
        liftsToFilter: List<LiftUiModel>,
        nameFilter: String?,
        movementPatternFilters: List<FilterChipOption>,
        liftIdFilters: Set<Long>,
    ): List<LiftUiModel> {
        return liftsToFilter.let { lifts ->
            val hasNameFilter = nameFilter?.isNotEmpty() == true
            val hasMovementPatternFilters = movementPatternFilters.isNotEmpty()
            val hasLiftIdFilter = liftIdFilters.isNotEmpty()

            if (hasNameFilter || hasMovementPatternFilters || hasLiftIdFilter) {
                lifts.filter { lift ->
                    val movementPatternFilter = FilterChipOption(MOVEMENT_PATTERN, lift.movementPatternDisplayName)
                    (!hasNameFilter || lift.name.contains(nameFilter, true)) &&
                            (!hasMovementPatternFilters || movementPatternFilters.contains(movementPatternFilter)) &&
                            (!hasLiftIdFilter || !liftIdFilters.contains(lift.id))
                }
            } else lifts
        }
    }

    fun applyFilters() = executeWithErrorHandling("Failed to apply filters") {
        val filteredLifts = getFilteredLifts(
            liftsToFilter = _state.value.allLifts,
            nameFilter = _state.value.nameFilter,
            movementPatternFilters = _state.value.movementPatternFilters,
            liftIdFilters = _state.value.liftsToFilterOut)

        _state.update {
            it.copy(
                showFilterSelection = false,
                filteredLifts = filteredLifts,
            )
        }
    }

    fun deleteLift(lift: LiftUiModel) = executeWithErrorHandling("Failed to delete lift") {
        deleteLiftUseCase(lift.toDomainModel())
    }
}