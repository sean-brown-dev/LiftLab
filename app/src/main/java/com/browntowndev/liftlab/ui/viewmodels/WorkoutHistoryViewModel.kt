package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.DATE_RANGE
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.PROGRAM
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.WORKOUT
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.common.toSimpleDateString
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutHistoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.time.ZoneId

class WorkoutHistoryViewModel(
    private val navHostController: NavHostController,
    private val loggingRepository: LoggingRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _loggingLiveData: LiveData<List<WorkoutLogEntryDto>>? = null
    private var _logObserver: Observer<List<WorkoutLogEntryDto>>? = null
    private val _state = MutableStateFlow(WorkoutHistoryState())
    val state = _state.asStateFlow()

    init {
        _logObserver = Observer { workoutLogs ->
            val dateOrderedWorkoutLogs = sortAndSetPersonalRecords(workoutLogs)
            val topSets = getTopSets(dateOrderedWorkoutLogs)
            _state.update {
                it.copy(
                    dateOrderedWorkoutLogs = dateOrderedWorkoutLogs,
                    filteredWorkoutLogs = dateOrderedWorkoutLogs,
                    topSets = topSets,
                )
            }
        }
        _loggingLiveData = loggingRepository.getAll()
        _loggingLiveData!!.observeForever(_logObserver!!)
    }

    override fun onCleared() {
        super.onCleared()
        _loggingLiveData?.removeObserver(_logObserver!!)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> onBackNavigationIconPressed()
            TopAppBarAction.EditDateRange -> toggleDateRangePicker()
            else -> { }
        }
    }

    fun setDateRangeFilter(start: Long?, end: Long?) {
        _state.update {
            it.copy(
                startDateInMillis = start,
                endDateInMillis = end,
            )
        }
    }

    fun toggleDateRangePicker() {
        _state.update {
            it.copy(isDatePickerVisible = !it.isDatePickerVisible)
        }

        if (!_state.value.isDatePickerVisible) {
            applyFilters()
        }
    }

    fun removeFilterChip(filterChip: FilterChipOption) {
        val isDateRangeChip = filterChip.type == DATE_RANGE
        val isWorkoutChip = filterChip.type == WORKOUT
        val isProgramChip = filterChip.type == PROGRAM
        _state.update {
            it.copy(
                startDateInMillis = if (isDateRangeChip) null else it.startDateInMillis,
                endDateInMillis = if (isDateRangeChip) null else it.endDateInMillis,
                programIdFilters = if (isProgramChip) {
                    it.programIdFilters.toMutableList().apply {
                        remove(filterChip.value.toLong())
                    }
                } else it.programIdFilters,
                workoutIdFilters = if (isWorkoutChip) {
                    it.workoutIdFilters.toMutableList().apply {
                        remove(filterChip.value.toLong())
                    }
                } else it.workoutIdFilters
            )
        }

        applyFilters()
    }

    private fun applyFilters() {
        _state.update { currentState ->
            currentState.copy(
                filteredWorkoutLogs = currentState.dateOrderedWorkoutLogs.filter { workoutLog ->
                    currentState.dateRangeFilter.contains(workoutLog.date.time) ||
                            currentState.workoutIdFilters.contains(workoutLog.workoutId) ||
                            currentState.programIdFilters.contains(workoutLog.programId)
                },
                filterChips = currentState.filterChips.toMutableList().apply {
                    clear()
                    if (currentState.startDateInMillis != null || currentState.endDateInMillis != null) {
                        val utcZoneId = ZoneId.of("UTC")
                        val firstDateInUtcMillis = currentState.dateRangeFilter.first
                        val secondDateInUtcMillis = currentState.endDateInMillis!!
                        val dateRange = "${firstDateInUtcMillis.toDate().toSimpleDateString(utcZoneId)} - " +
                                secondDateInUtcMillis.toDate().toSimpleDateString(utcZoneId)
                        add(FilterChipOption(type = DATE_RANGE, value = dateRange))
                    }
                    currentState.workoutIdFilters.fastForEach { workoutId ->
                        val workoutName = currentState.workoutNamesById[workoutId]
                        add(FilterChipOption(type = WORKOUT, value = workoutName!!))
                    }
                    currentState.programIdFilters.fastForEach { programId ->
                        val programName = currentState.programNamesById[programId]
                        add(FilterChipOption(type = PROGRAM, value = programName!!))
                    }
                }
            )
        }
    }

    private fun sortAndSetPersonalRecords(workoutLogs: List<WorkoutLogEntryDto>): List<WorkoutLogEntryDto> {
        val personalRecords = getPersonalRecords(workoutLogs)
        val updatedLogs = workoutLogs
            .sortedByDescending { it.date }
            .fastMap { workoutLog ->
                workoutLog.copy(
                    setResults = workoutLog.setResults.fastMap { setLog ->
                        if (personalRecords.contains(setLog)) {
                            setLog.copy(
                                isPersonalRecord = true
                            )
                        } else setLog
                    }
                )
            }

        return updatedLogs
    }

    private fun getPersonalRecords(workoutLogs: List<WorkoutLogEntryDto>): HashSet<SetLogEntryDto> {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults
                .groupBy { it.liftId }
                .map { liftSetResults ->
                    liftSetResults.value.maxBy {
                        CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                    }
                }
        }.toHashSet()
    }

    private fun getTopSets(workoutLogs: List<WorkoutLogEntryDto>): Map<Long, Map<Long, Pair<Int, SetLogEntryDto>>> {
        return workoutLogs.associate { workoutLog ->
            workoutLog.id to getTopSetsForWorkout(workoutLog)
        }
    }

    private fun getTopSetsForWorkout(workoutLog: WorkoutLogEntryDto): Map<Long, Pair<Int, SetLogEntryDto>> {
        return workoutLog.setResults
            .groupBy { it.liftId }
            .filterValues { set -> set.isNotEmpty() }
            .mapValues { (_, sets) ->
                val setSize = sets.size
                val topSet = sets.maxBy {
                    CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                }
                setSize to topSet
            }
    }

    private fun onBackNavigationIconPressed() {
        if (_state.value.isDatePickerVisible) {
            toggleDateRangePicker()
        } else {
            navHostController.popBackStack()
        }
    }
}