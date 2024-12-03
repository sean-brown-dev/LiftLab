package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
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
import com.browntowndev.liftlab.ui.views.navigation.Route
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
    private val onNavigateHome: () -> Unit,
    private val onNavigateToWorkoutBuilder: (workoutId: Long) -> Unit,
    private val onNavigateToActiveWorkout: () -> Unit,
    private val onNavigateToLiftDetails: (liftId: Long?) -> Unit,
    workoutId: Long?,
    addAtPosition: Int?,
    initialMovementPatternFilter: String,
    newLiftMetricChartIds: List<Long>,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _liftsLiveData: LiveData<List<LiftDto>>? = null
    private var _liftsObserver: Observer<List<LiftDto>>? = null
    private val _state = MutableStateFlow(LiftLibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    workoutId = workoutId,
                    addAtPosition = addAtPosition,
                    newLiftMetricChartIds = newLiftMetricChartIds,
                    liftIdFilters = getLiftIdFilters(workoutId),
                    movementPatternFilters = if(initialMovementPatternFilter.isNotEmpty()) {
                        listOf(FilterChipOption(type = MOVEMENT_PATTERN, value = initialMovementPatternFilter))
                    } else {
                        it.movementPatternFilters
                    }
                )
            }
        }

        _liftsLiveData = liftsRepository.getAllAsLiveData()
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
            TopAppBarAction.ConfirmAddLift -> if (_state.value.newLiftMetricChartIds.isEmpty()) addWorkoutLifts() else updateLiftMetricChartsWithSelectedLiftIds()
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

    private suspend fun getLiftIdFilters(
        workoutId: Long?,
    ): HashSet<Long> {
        return if (workoutId != null) {
            workoutLiftsRepository.getLiftIdsForWorkout(workoutId).toHashSet()
        } else hashSetOf()
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
            var liftMetricCharts = liftMetricChartRepository.getMany(_state.value.newLiftMetricChartIds)

            liftMetricCharts = newLiftIds.flatMap { currLiftId ->
                liftMetricCharts.fastMap { chart ->
                    updateChart(chart, currLiftId, newLiftIds.first())
                }
            }

            liftMetricChartRepository.upsertMany(liftMetricCharts = liftMetricCharts)
            onNavigateHome()
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
                        liftNote = null,
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
        callerRouteId: Long,
    ) {
        _state.update { it.copy(replacingLift = true) }

        viewModelScope.launch {
            workoutLiftsRepository.updateLiftId(workoutLiftId = workoutLiftId, newLiftId = replacementLiftId)
            if (callerRouteId == Route.WorkoutBuilder.id) {
                navigateBackToWorkoutBuilder()
            } else {
                navigateBackToActiveWorkout()
            }
        }
    }

    private fun navigateBackToWorkoutBuilder() {
        onNavigateToWorkoutBuilder(_state.value.workoutId!!)
    }

    private fun navigateBackToActiveWorkout() {
        onNavigateToActiveWorkout()
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
        val liftIdFilters = _state.value.liftIdFilters

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
}