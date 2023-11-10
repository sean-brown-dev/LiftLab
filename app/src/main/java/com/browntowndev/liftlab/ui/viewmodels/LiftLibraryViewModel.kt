package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.MOVEMENT_PATTERN
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftMetricChartRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLibraryState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.HomeScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
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
    private val liftMetricChartRepository: LiftMetricChartRepository,
    private val navHostController: NavHostController,
    workoutId: Long?,
    addAtPosition: Int?,
    initialMovementPatternFilter: String,
    liftMetricChartIds: List<Long>,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _liftsLiveData: LiveData<List<LiftDto>>? = null
    private var _liftsObserver: Observer<List<LiftDto>>? = null
    private val _state = MutableStateFlow(LiftLibraryState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                workoutId = workoutId,
                addAtPosition = addAtPosition,
                liftMetricChartIds = liftMetricChartIds,
                movementPatternFilters = if(initialMovementPatternFilter.isNotEmpty()) {
                    listOf(FilterChipOption(type = MOVEMENT_PATTERN, value = initialMovementPatternFilter))
                } else {
                    it.movementPatternFilters
                }
            )
        }

        _liftsLiveData = liftsRepository.getAll()
        _liftsObserver = Observer { lifts ->
            val sortedLifts = lifts.sortedBy { it.name }
            _state.update { currentState ->
                currentState.copy(
                    allLifts = sortedLifts,
                    filteredLifts = getFilteredLifts(sortedLifts)
                )
            }
        }

        _liftsLiveData!!.observeForever(_liftsObserver!!)
    }

    override fun onCleared() {
        super.onCleared()
        _liftsLiveData?.removeObserver(_liftsObserver!!)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(event: TopAppBarEvent.ActionEvent) {
        when (event.action) {
            TopAppBarAction.FilterStarted -> toggleFilterSelection()
            TopAppBarAction.NavigatedBack -> if (_state.value.showFilterSelection) applyFilters()
            TopAppBarAction.ConfirmAddLift -> if (_state.value.liftMetricChartIds.isEmpty()) addWorkoutLifts() else updateLiftMetricChartsWithSelectedLiftIds()
            TopAppBarAction.CreateNewLift -> navigateToCreateLiftMenu()
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

    private fun updateLiftMetricChartsWithSelectedLiftIds() {
        viewModelScope.launch {
            val newLiftIds = _state.value.selectedNewLiftsHashSet
            var liftMetricCharts = liftMetricChartRepository.getMany(_state.value.liftMetricChartIds)

            liftMetricCharts = newLiftIds.flatMap { currLiftId ->
                liftMetricCharts.fastMap { chart ->
                    updateChart(chart, currLiftId, newLiftIds.first())
                }
            }

            liftMetricChartRepository.upsertMany(liftMetricCharts = liftMetricCharts)
            navigateBackToHome()
        }
    }

    private fun updateChart(chart: LiftMetricChartDto, liftId: Long, firstLiftId: Long): LiftMetricChartDto {
        return chart.copy(
            id = if (liftId == firstLiftId) chart.id else 0L,
            liftId = liftId
        )
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
                        liftVolumeTypes = newLift.volumeTypesBitmask,
                        liftSecondaryVolumeTypes = newLift.secondaryVolumeTypesBitmask,
                        position = position,
                        deloadWeek = null,
                        setCount = 3,
                        incrementOverride = newLift.incrementOverride,
                        restTime = newLift.restTime,
                        restTimerEnabled = newLift.restTimerEnabled,
                        rpeTarget = 8f,
                        repRangeBottom = 8,
                        repRangeTop = 10,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
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
        _state.update { it.copy(replacingLift = true) }

        viewModelScope.launch {
            workoutLiftsRepository.updateLiftId(workoutLiftId = workoutLiftId, newLiftId = replacementLiftId)
            navigateBackToWorkoutBuilder()
        }
    }

    private fun navigateBackToHome() {
        // Pop back to before Home
        while (navHostController.previousBackStackEntry?.destination?.route != HomeScreen.navigation.route) {
            navHostController.popBackStack()
        }

        // Go back to Home
        navHostController.navigate(HomeScreen.navigation.route)
    }

    private fun navigateBackToWorkoutBuilder() {
        // Pop back to lab
        while (navHostController.currentBackStackEntry?.destination?.route != LabScreen.navigation.route) {
            navHostController.popBackStack()
        }

        // Go back to workout builder
        val workoutBuilderRoute = WorkoutBuilderScreen.navigation.route.replace("{id}", _state.value.workoutId.toString())
        navHostController.navigate(workoutBuilderRoute)
    }

    private fun toggleFilterSelection() {
        _state.update {
            it.copy(
                showFilterSelection = !_state.value.showFilterSelection
            )
        }
    }

    private fun setNameFilter(filter: String) {
        _state.update {
            it.copy(nameFilter = filter)
        }

        applyFilters()
    }

    fun addMovementPatternFilter(movementPattern: FilterChipOption) {
        _state.update {
            it.copy(
                movementPatternFilters = it.movementPatternFilters
                    .toMutableList()
                    .apply { add(movementPattern) }
            )
        }
    }

    fun removeMovementPatternFilter(movementPattern: FilterChipOption, apply: Boolean) {
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

    private fun getFilteredLifts(liftsToFilter: List<LiftDto>): List<LiftDto> {
        val nameFilter = _state.value.nameFilter
        val movementPatternFilters = _state.value.movementPatternFilters

        return liftsToFilter.let { lifts ->
            val hasNameFilter = nameFilter?.isNotEmpty() == true
            val hasMovementPatternFilters = movementPatternFilters.isNotEmpty()

            if (hasNameFilter || hasMovementPatternFilters) {
                lifts.filter { lift ->
                    val nameMatches = if (hasNameFilter) {
                        lift.name.contains(nameFilter!!, true)
                    } else true

                    nameMatches && if (hasMovementPatternFilters) {
                        val movementPatternFilter = FilterChipOption(
                            type = MOVEMENT_PATTERN,
                            value = lift.movementPatternDisplayName
                        )
                        movementPatternFilters.contains(movementPatternFilter)
                    } else true
                }
            } else lifts
        }

    }

    fun applyFilters() {
        val filteredLifts = getFilteredLifts(_state.value.allLifts)
        _state.update {
            it.copy(
                showFilterSelection = false,
                filteredLifts = filteredLifts,
            )
        }
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