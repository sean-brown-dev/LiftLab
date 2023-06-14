package com.browntowndev.liftlab.ui.views.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.views.utils.ConfirmationModal
import com.browntowndev.liftlab.ui.views.utils.IconDropdown
import com.browntowndev.liftlab.ui.views.utils.TextFieldModal
import com.browntowndev.liftlab.ui.views.utils.EventBusDisposalEffect
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.koin.androidx.compose.koinViewModel

@ExperimentalFoundationApi
@Composable
fun Lab(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit
) {
    val labViewModel: LabViewModel = koinViewModel()
    val labState by labViewModel.state.collectAsState()

    LaunchedEffect(labState.program != null) {
        mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, labState.originalProgramName))
        setTopAppBarControlVisibility(LabScreen.RENAME_PROGRAM_ICON, labState.program != null)
        setTopAppBarControlVisibility(LabScreen.DELETE_PROGRAM_ICON, labState.program != null)
        labViewModel.registerEventBus()
    }

    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = labViewModel)

    BackHandler(labState.isReordering) {
        labViewModel.toggleReorderingScreen()
    }

    if (!labState.isReordering) {
        setTopAppBarCollapsed(false)
        setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, false)
        setTopAppBarControlVisibility(Screen.OVERFLOW_MENU_ICON, true)

        if (labState.program?.workouts != null) {
            WorkoutCardList(
                workouts = labState.program!!.workouts,
                showEditWorkoutNameModal = { workout -> labViewModel.showEditWorkoutNameModal(workout) },
                beginDeleteWorkout = { labViewModel.beginDeleteWorkout(it) },
                paddingValues = paddingValues,
                navigationController = navHostController
            )
        }
    }
    else {
        setTopAppBarCollapsed(true)
        setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, true)
        setTopAppBarControlVisibility(Screen.OVERFLOW_MENU_ICON, false)

        if (labState.program?.workouts != null) {
            DraggableWorkoutList(
                paddingValues = paddingValues,
                workouts = labState.program!!.workouts,
                changePosition = { to, from -> labViewModel.changePosition(to, from) },
                saveReorder = { labViewModel.saveReorder() },
                toggleReorderingScreen = { labViewModel.toggleReorderingScreen() },
            )
        }
    }

    if (labState.workoutIdToRename != null && labState.originalWorkoutName != null) {
        TextFieldModal(
            header = "Rename ${labState.originalWorkoutName}",
            initialTextFieldValue = labState.originalWorkoutName!!,
            onConfirm = { labViewModel.updateWorkoutName(labState.workoutIdToRename!!, it) },
            onCancel = { labViewModel.collapseEditWorkoutNameModal() },
        )
    }

    if (labState.isEditingProgramName && labState.program != null) {
        TextFieldModal(
            header = "Rename ${labState.program!!.name}",
            initialTextFieldValue = labState.program!!.name,
            onConfirm = { labViewModel.updateProgramName(it) },
            onCancel = { labViewModel.collapseEditProgramNameModal() }
        )
    }

    if (labState.isDeletingProgram && labState.program != null) {
        ConfirmationModal(
            header = "Delete?",
            body = "Are you sure you want to delete ${labState.program!!.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteProgram() },
            onCancel = { labViewModel.cancelDeleteProgram() }
        )
    }

    if (labState.workoutToDelete != null) {
        ConfirmationModal(
            header = "Delete?",
            body = "Are you sure you want to delete ${labState.workoutToDelete!!.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteWorkout(labState.workoutToDelete!!) },
            onCancel = { labViewModel.cancelDeleteWorkout() }
        )
    }
}

@Composable
fun WorkoutCardList(
    paddingValues: PaddingValues,
    navigationController: NavHostController,
    workouts: List<WorkoutDto>,
    showEditWorkoutNameModal: (String) -> Unit,
    beginDeleteWorkout: (WorkoutDto) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.Center)
            .padding(paddingValues),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(workouts, { it.id }) { workout ->
            WorkoutCard(
                workoutName = workout.name,
                workoutId = workout.id,
                lifts = workout.lifts,
                navigationController = navigationController,
                showEditWorkoutNameModal = showEditWorkoutNameModal,
                beginDeleteWorkout = { beginDeleteWorkout(workout) },
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun DraggableWorkoutList(
    workouts: List<WorkoutDto>,
    changePosition: (Int, Int) -> Unit,
    saveReorder: () -> Unit,
    toggleReorderingScreen: () -> Unit,
    paddingValues: PaddingValues
) {
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            changePosition(to.index -1, from.index - 1)
        },
    )

    LazyColumn(
        state = reorderableState.listState,
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .reorderable(reorderableState)
            .detectReorderAfterLongPress(reorderableState),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "Press, Hold & Drag to Reorder",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(25.dp))
            }
        }
        items(workouts, { it.id }) { workout ->
            ReorderableItem(reorderableState = reorderableState, key = workout.id) { isDragging ->
                val elevation: State<Dp> = if (isDragging) animateDpAsState(16.dp) else animateDpAsState(0.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp)
                        .shadow(
                            elevation = elevation.value,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = MaterialTheme.colorScheme.primary,
                            spotColor = MaterialTheme.colorScheme.primary,
                            clip = true
                        )
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = workout.name,
                        modifier = Modifier
                            .padding(0.dp, 25.dp)
                            .offset(x = (-10).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        item {
            Spacer(modifier = Modifier.height(15.dp))
            Column (
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 15.dp, 0.dp)
                        .clickable { saveReorder() },
                    text = "Confirm Reorder",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 15.dp, 0.dp)
                        .clickable { toggleReorderingScreen() },
                    text = "Cancel Reorder",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun WorkoutCard(
    modifier: Modifier = Modifier,
    navigationController: NavHostController,
    workoutId: Long,
    workoutName: String,
    lifts: List<GenericWorkoutLift>,
    showEditWorkoutNameModal: (String) -> Unit,
    beginDeleteWorkout: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxSize()
            .padding(5.dp)
            .clickable {
                navigationController.navigate(WorkoutBuilderScreen.navigation.route + "/$workoutId")
            },
        shape = CardDefaults.shape,
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = workoutName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                modifier = Modifier.padding(15.dp, 0.dp, 0.dp, 0.dp)
            )
            WorkoutMenuDropdown(
                workoutName = workoutName,
                showEditWorkoutNameModal = showEditWorkoutNameModal,
                beginDeleteWorkout = beginDeleteWorkout,
            )
        }
        Divider(thickness = 12.dp, color = MaterialTheme.colorScheme.background)
        lifts.forEach {
            Text(
                text = "${it.setCount} x ${it.liftName}",
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }
        Divider(thickness = 15.dp, color = MaterialTheme.colorScheme.background)
    }
}

@Composable
fun WorkoutMenuDropdown(
    workoutName: String,
    showEditWorkoutNameModal: (String) -> Unit,
    beginDeleteWorkout: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    IconDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = isExpanded,
        onToggleExpansion = { isExpanded = !isExpanded }
    ) {
        DropdownMenuItem(
            text = { Text("Edit Name") },
            onClick = {
                isExpanded = false
                showEditWorkoutNameModal(workoutName)
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            })
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                isExpanded = false
                beginDeleteWorkout()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            })
    }
}