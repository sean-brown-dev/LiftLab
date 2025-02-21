package com.browntowndev.liftlab.ui.views.main.liftlibrary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.enums.MovementPatternFilterSection
import com.browntowndev.liftlab.ui.composables.CircledTextIcon
import com.browntowndev.liftlab.ui.composables.CircularIcon
import com.browntowndev.liftlab.ui.composables.DeleteableOnSwipeLeft
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.FilterSelector
import com.browntowndev.liftlab.ui.composables.InputChipFlowRow
import com.browntowndev.liftlab.ui.composables.verticalScrollbar
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.navigation.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun LiftLibrary(
    paddingValues: PaddingValues,
    screenId: String?,
    callerRouteId: Long?,
    onNavigateHome: () -> Unit,
    onNavigateToWorkoutBuilder: (workoutId: Long) -> Unit,
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToLiftDetails: (liftId: Long?) -> Unit,
    workoutId: Long? = null,
    workoutLiftId: Long? = null,
    movementPattern: String = "",
    liftMetricChartIds: List<Long>,
    addAtPosition: Int? = null,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    onClearTopAppBarFilterText: () -> Unit,
    onToggleTopAppBarControlVisibility: (controlName: String, visible: Boolean) -> Unit,
    onChangeTopAppBarTitle: (title: String) -> Unit,
) {
    val liftLibraryViewModel: LiftLibraryViewModel = koinViewModel {
        parametersOf(onNavigateHome, onNavigateToWorkoutBuilder, onNavigateToActiveWorkout, onNavigateToLiftDetails,
            workoutId, addAtPosition, movementPattern, liftMetricChartIds)
    }
    val state by liftLibraryViewModel.state.collectAsState()

    val isReplacingLiftInWorkoutBuilder = remember(key1 = workoutId, key2 = workoutLiftId, key3 = callerRouteId) {
        workoutId != null && workoutLiftId != null && callerRouteId == Route.WorkoutBuilder.id
    }
    val isReplacingLiftInWorkout = remember(key1 = workoutId, key2 = workoutLiftId, key3 = callerRouteId) {
        workoutId != null && workoutLiftId != null && callerRouteId == Route.Workout.id
    }
    val isAddingToWorkout = remember(key1 = workoutId, key2 = addAtPosition) {
        workoutId != null && addAtPosition != null
    }
    val isCreatingLiftMetricCharts = remember(liftMetricChartIds) { liftMetricChartIds.isNotEmpty() }

    liftLibraryViewModel.registerEventBus()
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = liftLibraryViewModel)

    LaunchedEffect(state.showFilterSelection) {
        onChangeTopAppBarTitle(if(state.showFilterSelection) "Filter Options" else LiftLibraryScreen.navigation.title)
        onToggleTopAppBarControlVisibility(Screen.NAVIGATION_ICON, state.showFilterSelection)
        onToggleTopAppBarControlVisibility(LiftLibraryScreen.SEARCH_ICON, !state.showFilterSelection)
        onToggleTopAppBarControlVisibility(LiftLibraryScreen.LIFT_MOVEMENT_PATTERN_FILTER_ICON, !state.showFilterSelection)
    }

    LaunchedEffect(key1 = state.selectedNewLifts, key2 = state.showFilterSelection) {
        val confirmAddVisible = state.selectedNewLifts.isNotEmpty() && !state.showFilterSelection
        onToggleTopAppBarControlVisibility(LiftLibraryScreen.CONFIRM_ADD_LIFT_ICON, confirmAddVisible)
    }

    if (!state.showFilterSelection && !state.replacingLift) {
        setTopAppBarCollapsed(false)
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            InputChipFlowRow(
                filters = state.allFilters,
                onRemove = {
                    if (it.type == FilterChipOption.NAME) {
                        onClearTopAppBarFilterText()
                    } else {
                        liftLibraryViewModel.removeMovementPatternFilter(it, true)
                    }
                },
            )
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.background)
                        .wrapContentSize(Alignment.TopStart)
                        .verticalScrollbar(
                            lazyListState = lazyListState,
                            onRequestBubbleTextForScrollLocation = { liftIndex ->
                                if (liftIndex > -1 && liftIndex < state.filteredLifts.size) {
                                    state.filteredLifts[liftIndex].name[0].toString()
                                } else ""
                            }
                        ),
                ) {
                    items(state.filteredLifts, { it.id }) { lift ->
                        val selected by remember(state.selectedNewLifts) {
                            mutableStateOf(state.selectedNewLiftsHashSet.contains(lift.id))
                        }
                        DeleteableOnSwipeLeft(
                            confirmationDialogHeader = "Delete Lift?",
                            confirmationDialogBody = "Deleting this lift will hide it from the Lifts menu. It can be restored from the Settings menu.",
                            enabled = !isAddingToWorkout && !isReplacingLiftInWorkoutBuilder && !isCreatingLiftMetricCharts && !isReplacingLiftInWorkout,
                            onDelete = { liftLibraryViewModel.hideLift(lift) },
                        ) {
                            ListItem(
                                modifier = Modifier.clickable {
                                    val multiselectEnabled = isAddingToWorkout || isCreatingLiftMetricCharts
                                    if(multiselectEnabled && selected) {
                                        liftLibraryViewModel.removeSelectedLift(lift.id)
                                    } else if (multiselectEnabled) {
                                        liftLibraryViewModel.addSelectedLift(lift.id)
                                    } else if (isReplacingLiftInWorkoutBuilder || isReplacingLiftInWorkout) {
                                        liftLibraryViewModel.replaceWorkoutLift(
                                            workoutLiftId = workoutLiftId!!,
                                            replacementLiftId = lift.id,
                                            callerRouteId = callerRouteId!!
                                        )
                                    } else {
                                        onNavigateToLiftDetails(lift.id)
                                    }
                                },
                                headlineContent = { Text(lift.name) },
                                supportingContent = { Text(lift.movementPatternDisplayName) },
                                leadingContent = {
                                    if (selected) {
                                        CircularIcon(
                                            size = 40.dp,
                                            imageVector = Icons.Filled.Check,
                                            circleBackgroundColorScheme = MaterialTheme.colorScheme.tertiary,
                                            iconTint = MaterialTheme.colorScheme.secondary,
                                        )
                                    } else {
                                        CircledTextIcon(text = lift.name[0].toString())
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    headlineColor = MaterialTheme.colorScheme.onBackground,
                                    supportingColor = MaterialTheme.colorScheme.onBackground,
                                    leadingIconColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }
                }
            }
        }
    } else if (!state.replacingLift) {
        setTopAppBarCollapsed(true)
        FilterSelector(
            modifier = Modifier.padding(paddingValues),
            filterOptionSections = remember {
                listOf(
                    MovementPatternFilterSection.UpperCompound,
                    MovementPatternFilterSection.UpperAccessory,
                    MovementPatternFilterSection.LowerCompound,
                    MovementPatternFilterSection.LowerAccessory,
                )
            },
            selectedFilters = state.movementPatternFilters,
            onAddFilter = { liftLibraryViewModel.addMovementPatternFilter(it) },
            onRemoveFilter = { liftLibraryViewModel.removeMovementPatternFilter(it, false) },
            onConfirmSelections = {
                liftLibraryViewModel.applyFilters()
            },
        )
    }
}
