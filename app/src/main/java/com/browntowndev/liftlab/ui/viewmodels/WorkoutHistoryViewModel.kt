package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.DATE_RANGE
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.PROGRAM
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.WORKOUT
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.common.toMediumDateString
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetSummarizedWorkoutMetricsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteWorkoutLogEntryUseCase
import com.browntowndev.liftlab.ui.factory.createProgramAndWorkoutFilterChipOptions
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutHistoryState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.time.ZoneId

class WorkoutHistoryViewModel(
    getSummarizedWorkoutMetricsStateFlowUseCase: GetSummarizedWorkoutMetricsStateFlowUseCase,
    private val deleteWorkoutLogEntryUseCase: DeleteWorkoutLogEntryUseCase,
    private val onNavigateBack: () -> Unit,
    eventBus: EventBus,
): BaseViewModel(eventBus) {
    private val _state = MutableStateFlow(WorkoutHistoryState())
    val state = _state.asStateFlow()

    init {
        getSummarizedWorkoutMetricsStateFlowUseCase()
            .distinctUntilChanged()
            .map { summarizedWorkoutMetricsState ->
                val dateOrderedWorkoutLogs = summarizedWorkoutMetricsState.dateOrderedWorkoutLogsWithPersonalRecords
                val topSets = summarizedWorkoutMetricsState.topSets
                val workoutNamesById = dateOrderedWorkoutLogs
                    .distinctBy { workoutLog -> workoutLog.workoutId }
                    .sortedBy { it.workoutName }
                    .associate { workoutLog ->
                        workoutLog.workoutId to workoutLog.workoutName
                    }
                val programNamesById = dateOrderedWorkoutLogs
                    .distinctBy { workoutLog -> workoutLog.programId }
                    .sortedBy { it.programName }
                    .associate { workoutLog ->
                        workoutLog.programId to workoutLog.programName
                    }
                WorkoutHistoryState(
                    dateOrderedWorkoutLogs = dateOrderedWorkoutLogs,
                    filteredWorkoutLogs = dateOrderedWorkoutLogs,
                    topSets = topSets,
                    workoutNamesById = workoutNamesById,
                    programNamesById = programNamesById
                )
            }.onEach { newState ->
                _state.update {
                    it.copy(
                        dateOrderedWorkoutLogs = newState.dateOrderedWorkoutLogs,
                        filteredWorkoutLogs = newState.dateOrderedWorkoutLogs,
                        topSets = newState.topSets,
                        workoutNamesById = newState.workoutNamesById,
                        programNamesById = newState.programNamesById,
                        programAndWorkoutFilterSections = createProgramAndWorkoutFilterChipOptions(
                            programNamesById = newState.programNamesById,
                            workoutNamesById = newState.workoutNamesById
                        )
                    )
                }
            }.catch {
                Log.e("WorkoutHistoryViewModel", "Error getting workout logs", it)
                FirebaseCrashlytics.getInstance().recordException(it)
                emitUserMessage("Failed to load workout history.")
            }.launchIn(viewModelScope)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> onBackNavigationIconPressed()
            TopAppBarAction.EditDateRange -> toggleDateRangePicker()
            TopAppBarAction.FilterStarted -> _state.update { it.copy(isProgramAndWorkoutFilterVisible = true) }
            else -> { }
        }
    }

    fun setDateRangeFilter(start: Long?, end: Long?) = executeWithErrorHandling("Failed to set date range filter") {
        _state.update {
            it.copy(
                startDateInMillis = start,
                endDateInMillis = end,
            )
        }
    }

    fun toggleDateRangePicker() = executeWithErrorHandling("Failed to toggle date range picker") {
        _state.update {
            it.copy(isDatePickerVisible = !it.isDatePickerVisible)
        }

        if (!_state.value.isDatePickerVisible) {
            applyFilters()
        }
    }

    fun addWorkoutOrProgramFilter(filterChip: FilterChipOption) = executeWithErrorHandling("Failed to add filter chip") {
        _state.update {
            it.copy(
                programAndWorkoutFilters = it.programAndWorkoutFilters.toMutableList().apply {
                    add(filterChip)
                }
            )
        }
    }

    fun removeWorkoutOrProgramFilter(toRemove: FilterChipOption) = executeWithErrorHandling("Failed to remove filter chip") {
        _state.update {
            it.copy(
                programAndWorkoutFilters = it.programAndWorkoutFilters.toMutableList().apply {
                    remove(toRemove)
                }
            )
        }
    }

    fun removeFilterChip(toRemove: FilterChipOption) = executeWithErrorHandling("Failed to remove filter chip") {
        val isDateRangeChip = toRemove.type == DATE_RANGE
        _state.update {
            it.copy(
                startDateInMillis = if (isDateRangeChip) null else it.startDateInMillis,
                endDateInMillis = if (isDateRangeChip) null else it.endDateInMillis,
                programAndWorkoutFilters = if (!isDateRangeChip) {
                    it.programAndWorkoutFilters.toMutableList().apply { remove(toRemove) }
                } else it.programAndWorkoutFilters
            )
        }

        applyFilters()
    }

    private fun isInProgramFilters(state: WorkoutHistoryState, workoutLog: WorkoutLogEntry): Boolean {
        return state.programAndWorkoutFilters.none { it.type == PROGRAM } ||
                state.programAndWorkoutFilters.fastAny { it.key == workoutLog.programId }
    }

    private fun isInWorkoutFilters(state: WorkoutHistoryState, workoutLog: WorkoutLogEntry): Boolean {
        return state.programAndWorkoutFilters.none { it.type == WORKOUT } ||
                state.programAndWorkoutFilters.fastAny { it.key == workoutLog.workoutId }
    }

    fun applyFilters() = executeWithErrorHandling("Failed to apply filters") {
        _state.update { currentState ->
            currentState.copy(
                isProgramAndWorkoutFilterVisible = false,
                filteredWorkoutLogs = currentState.dateOrderedWorkoutLogs.filter { workoutLog ->
                    currentState.dateRangeFilter.contains(workoutLog.date.time) &&
                            isInProgramFilters(currentState, workoutLog) &&
                            isInWorkoutFilters(currentState, workoutLog)
                },
                filterChips = currentState.filterChips.toMutableList().apply {
                    clear()
                    if (currentState.startDateInMillis != null || currentState.endDateInMillis != null) {
                        val utcZoneId = ZoneId.of("UTC")
                        val firstDateInUtcMillis = currentState.dateRangeFilter.first
                        val secondDateInUtcMillis = currentState.endDateInMillis!!
                        val dateRange = "${firstDateInUtcMillis.toDate().toMediumDateString(utcZoneId)} - " +
                                secondDateInUtcMillis.toDate().toMediumDateString(utcZoneId)
                        add(FilterChipOption(type = DATE_RANGE, value = dateRange))
                    }
                    currentState.programAndWorkoutFilters.fastForEach { filterChip ->
                        add(filterChip)
                    }
                }
            )
        }
    }

    private fun onBackNavigationIconPressed() {
        if (_state.value.isDatePickerVisible) {
            toggleDateRangePicker()
        } else if (_state.value.isProgramAndWorkoutFilterVisible) {
            applyFilters()
        } else {
            onNavigateBack()
        }
    }

    fun delete(id: Long) = executeWithErrorHandling("Failed to delete workout log") {
        deleteWorkoutLogEntryUseCase(id)
    }
}