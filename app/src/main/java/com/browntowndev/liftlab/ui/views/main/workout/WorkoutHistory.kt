package com.browntowndev.liftlab.ui.views.main.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.MovementPatternFilterSection
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.ui.viewmodels.WorkoutHistoryViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.EditWorkoutScreen
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.composables.FilterSelector
import com.browntowndev.liftlab.ui.views.composables.InputChipFlowRow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistory(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    setTopAppBarCollapsed: (Boolean) -> Unit,
) {
    val workoutHistoryViewModel: WorkoutHistoryViewModel = koinViewModel {
        parametersOf(navHostController)
    }
    val state by workoutHistoryViewModel.state.collectAsState()
    workoutHistoryViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = workoutHistoryViewModel)

    BackHandler(state.isDatePickerVisible) {
        workoutHistoryViewModel.toggleDateRangePicker()
    }

    if (state.isDatePickerVisible) {
        setTopAppBarCollapsed(true)
        val oldestLogEntry = remember(state.dateOrderedWorkoutLogs) { state.dateOrderedWorkoutLogs.lastOrNull()?.date }
        val newestLogEntry = remember(state.dateOrderedWorkoutLogs) { state.dateOrderedWorkoutLogs.firstOrNull()?.date }
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.startDateInMillis ?: oldestLogEntry?.time,
            initialSelectedEndDateMillis = state.endDateInMillis ?: newestLogEntry?.time,
            initialDisplayMode = DisplayMode.Picker,
            yearRange = remember(key1 = oldestLogEntry, key2 = newestLogEntry) {
                (oldestLogEntry?.toLocalDate()?.year ?: 2023)..(newestLogEntry?.toLocalDate()?.year ?: 2023)
            }
        )

        LaunchedEffect(
            key1 = dateRangePickerState.selectedStartDateMillis,
            key2 = dateRangePickerState.selectedEndDateMillis
        ) {
            workoutHistoryViewModel.setDateRangeFilter(
                start = dateRangePickerState.selectedStartDateMillis,
                end = dateRangePickerState.selectedEndDateMillis,
            )
        }

        DateRangePicker(
            modifier = Modifier.padding(paddingValues),
            state = dateRangePickerState,
            showModeToggle = false,
            title = {
                Text(
                    modifier = Modifier.padding(start = 65.dp),
                    text = "Filter Workouts"
                )
            }
        )
    } else if (state.isProgramAndWorkoutFilterVisible) {
        setTopAppBarCollapsed(true)
        FilterSelector(
            modifier = Modifier.padding(paddingValues),
            filterOptionSections = state.programAndWorkoutFilterSections,
            selectedFilters = state.programAndWorkoutFilters,
            onAddFilter = { workoutHistoryViewModel.addWorkoutOrProgramFilter(it) },
            onRemoveFilter = { workoutHistoryViewModel.removeWorkoutOrProgramFilter(it) },
            onConfirmSelections = {
                workoutHistoryViewModel.applyFilters()
            },
        )
    } else {
        setTopAppBarCollapsed(false)
        LazyColumn(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item {
                InputChipFlowRow(
                    filters = state.filterChips,
                    onRemove = {
                        workoutHistoryViewModel.removeFilterChip(it)
                    }
                )
            }
            items(state.filteredWorkoutLogs) { workoutLog ->
                WorkoutHistoryCard(
                    workoutName = workoutLog.workoutName,
                    workoutDate = workoutLog.date,
                    workoutDuration = workoutLog.durationInMillis,
                    setResults = workoutLog.setResults,
                    topSets = state.topSets[workoutLog.id],
                    onEditWorkout = {
                        val editWorkoutRoute = EditWorkoutScreen.navigation.route.replace("{workoutLogEntryId}", workoutLog.id.toString())
                        navHostController.navigate(editWorkoutRoute)
                    }
                )
            }
        }
    }
}