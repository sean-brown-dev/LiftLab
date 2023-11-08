package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.DATE_RANGE
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.PROGRAM
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.WORKOUT
import com.browntowndev.liftlab.core.common.FlowRowFilterChipSection
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
            val workoutNamesById = dateOrderedWorkoutLogs
                .distinctBy { workoutLog -> workoutLog.workoutId }
                .associate { workoutLog ->
                    workoutLog.workoutId to workoutLog.workoutName
                }
            val programNamesById = dateOrderedWorkoutLogs
                .distinctBy { workoutLog -> workoutLog.programId }
                .associate { workoutLog ->
                    workoutLog.programId to workoutLog.programName
                }
            _state.update {
                it.copy(
                    dateOrderedWorkoutLogs = dateOrderedWorkoutLogs,
                    filteredWorkoutLogs = dateOrderedWorkoutLogs,
                    topSets = topSets,
                    workoutNamesById = workoutNamesById,
                    programNamesById = programNamesById,
                    programAndWorkoutFilterSections = listOf(
                        object : FlowRowFilterChipSection {
                            override val sectionName: String
                                get() = "Programs"
                            override val filterChipOptions: Lazy<List<FilterChipOption>>
                                get() = lazy {
                                    programNamesById.map { program ->
                                        FilterChipOption(type = PROGRAM, value = program.value, key = program.key)
                                    }
                                }
                        },
                        object : FlowRowFilterChipSection {
                            override val sectionName: String
                                get() = "Workouts"
                            override val filterChipOptions: Lazy<List<FilterChipOption>>
                                get() = lazy {
                                    workoutNamesById.map { workout ->
                                        FilterChipOption(type = WORKOUT, value = workout.value, key = workout.key)
                                    }
                                }
                        },
                    )
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
            TopAppBarAction.FilterStarted -> _state.update { it.copy(isProgramAndWorkoutFilterVisible = true) }
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

    fun addWorkoutOrProgramFilter(filterChip: FilterChipOption) {
        _state.update {
            it.copy(
                programAndWorkoutFilters = it.programAndWorkoutFilters.toMutableList().apply {
                    add(filterChip)
                }
            )
        }
    }

    fun removeWorkoutOrProgramFilter(toRemove: FilterChipOption) {
        _state.update {
            it.copy(
                programAndWorkoutFilters = it.programAndWorkoutFilters.toMutableList().apply {
                    remove(toRemove)
                }
            )
        }
    }

    fun removeFilterChip(toRemove: FilterChipOption) {
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

    fun applyFilters() {
        _state.update { currentState ->
            currentState.copy(
                isProgramAndWorkoutFilterVisible = false,
                filteredWorkoutLogs = currentState.dateOrderedWorkoutLogs.filter { workoutLog ->
                    currentState.dateRangeFilter.contains(workoutLog.date.time) &&
                            (currentState.programAndWorkoutFilters.isEmpty() ||
                                    currentState.programAndWorkoutFilters.fastAny {
                                        val workoutMatches =
                                            it.type == WORKOUT && it.key == workoutLog.workoutId
                                        val programMatches =
                                            it.type == PROGRAM && it.key == workoutLog.programId
                                        workoutMatches || programMatches
                                    })
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
                    currentState.programAndWorkoutFilters.fastForEach { filterChip ->
                        add(filterChip)
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
        } else if (_state.value.isProgramAndWorkoutFilterVisible) {
            applyFilters()
        } else {
            navHostController.popBackStack()
        }
    }
}