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
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.MOVEMENT_PATTERN
import com.browntowndev.liftlab.core.common.enums.MovementPatternFilterSection
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftDetailsScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.composables.CircledTextIcon
import com.browntowndev.liftlab.ui.views.composables.CircularIcon
import com.browntowndev.liftlab.ui.views.composables.DeleteableOnSwipeLeft
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.composables.FilterSelector
import com.browntowndev.liftlab.ui.views.composables.InputChipFlowRow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun LiftLibrary(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    isSearchBarVisible: Boolean,
    workoutId: Long? = null,
    workoutLiftId: Long? = null,
    movementPattern: String = "",
    addAtPosition: Int? = null,
    onNavigateBack: () -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    onClearTopAppBarFilterText: () -> Unit,
    onToggleTopAppBarControlVisibility: (controlName: String, visible: Boolean) -> Unit,
    onChangeTopAppBarTitle: (title: String) -> Unit,
) {
    val liftLibraryViewModel: LiftLibraryViewModel = koinViewModel { parametersOf(navHostController) }
    val state by liftLibraryViewModel.state.collectAsState()

    liftLibraryViewModel.setWorkoutId(workoutId)
    liftLibraryViewModel.setAddAtPosition(addAtPosition)

    val isReplacingWorkout by remember(key1 = workoutId, key2 = workoutLiftId) {
        mutableStateOf(workoutId != null && workoutLiftId != null)
    }
    val isAddingToWorkout by remember(key1 = workoutId, key2 = addAtPosition) {
        mutableStateOf(workoutId != null && addAtPosition != null)
    }

    liftLibraryViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = liftLibraryViewModel)

    BackHandler(isSearchBarVisible) {
        onNavigateBack.invoke()
    }

    LaunchedEffect(state.showFilterSelection) {
        onChangeTopAppBarTitle(if(state.showFilterSelection) "Filter Options" else LiftLibraryScreen.navigation.title)
        onToggleTopAppBarControlVisibility(Screen.NAVIGATION_ICON, state.showFilterSelection)
        onToggleTopAppBarControlVisibility(LiftLibraryScreen.SEARCH_ICON, !state.showFilterSelection)
        onToggleTopAppBarControlVisibility(LiftLibraryScreen.LIFT_MOVEMENT_PATTERN_FILTER_ICON, !state.showFilterSelection)
    }

    LaunchedEffect(key1 = movementPattern) {
        if (movementPattern.isNotEmpty()) {
            liftLibraryViewModel.addMovementPatternFilter(FilterChipOption(type = MOVEMENT_PATTERN, value = movementPattern))
            liftLibraryViewModel.applyFilters()
        }
    }

    LaunchedEffect(key1 = state.selectedNewLifts) {
        if (state.selectedNewLifts.isEmpty()) {
            onToggleTopAppBarControlVisibility(LiftLibraryScreen.CONFIRM_ADD_LIFT_ICON, false)
        } else if (state.selectedNewLifts.size == 1) {
            onToggleTopAppBarControlVisibility(LiftLibraryScreen.CONFIRM_ADD_LIFT_ICON, true)
        }
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
                        liftLibraryViewModel.removeMovementPatternFilter(it)
                    }
                },
            )
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.background)
                        .wrapContentSize(Alignment.TopStart)
                ) {
                    items(state.filteredLifts, { it.id }) { lift ->
                        val selected by remember(state.selectedNewLifts) {
                            mutableStateOf(state.selectedNewLiftsHashSet.contains(lift.id))
                        }
                        DeleteableOnSwipeLeft(
                            confirmationDialogHeader = "Delete Lift?",
                            confirmationDialogBody = "Deleting this lift will hide it from the Lifts menu. It can be restored from the Settings menu.",
                            enabled = !isAddingToWorkout && !isReplacingWorkout,
                            onDelete = { liftLibraryViewModel.hideLift(lift) },
                        ) {
                            ListItem(
                                modifier = Modifier.clickable {
                                    if(isAddingToWorkout && selected) {
                                        liftLibraryViewModel.removeSelectedLift(lift.id)
                                    } else if (isAddingToWorkout) {
                                        liftLibraryViewModel.addSelectedLift(lift.id)
                                    } else if (isReplacingWorkout) {
                                        liftLibraryViewModel.replaceWorkoutLift(workoutLiftId!!, lift.id)
                                    } else {
                                        val liftDetailsRoute = LiftDetailsScreen.navigation.route
                                            .replace("{id}", lift.id.toString())

                                        navHostController.navigate(liftDetailsRoute)
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
            onRemoveFilter = { liftLibraryViewModel.removeMovementPatternFilter(it) },
            onConfirmSelections = {
                liftLibraryViewModel.applyFilters()
            },
        )
    }
}
