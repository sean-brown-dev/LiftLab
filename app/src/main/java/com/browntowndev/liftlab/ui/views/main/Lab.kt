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
import androidx.compose.material.icons.outlined.Add
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
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.TopAppBarViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.LabState
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.topAppBar.Screen
import com.browntowndev.liftlab.ui.views.utils.ConfirmationModal
import com.browntowndev.liftlab.ui.views.utils.MoreVertDropdown
import com.browntowndev.liftlab.ui.views.utils.TextFieldModal
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.koin.androidx.compose.getViewModel

@ExperimentalFoundationApi
@Composable
fun Lab(
    paddingValues: PaddingValues,
    labViewModel: LabViewModel = getViewModel(),
    topAppBarState: LiftLabTopAppBarState,
    topAppBarViewModel: TopAppBarViewModel,
) {
    val labState by labViewModel.state.collectAsState()
    val screen: LabScreen? = topAppBarState.currentScreen as LabScreen

    LaunchedEffect(key1 = screen) {
        labViewModel.watchActionBarActions(screen)
    }

    BackHandler(labState.isReordering) {
        labViewModel.toggleReorderingScreen()
    }

    if (!labState.isReordering) {
        topAppBarViewModel.setCollapsed(false)
        if (topAppBarState.navigationIconVisible == true) {
            topAppBarViewModel.toggleControlVisibility(Screen.NAVIGATION_ICON)
        }
        WorkoutCardList(labState = labState, labViewModel = labViewModel, paddingValues = paddingValues)
    }
    else {
        topAppBarViewModel.setCollapsed(true)
        if (topAppBarState.navigationIconVisible == false) {
            topAppBarViewModel.toggleControlVisibility(Screen.NAVIGATION_ICON)
        }
        DraggableWorkoutList(labState = labState, labViewModel = labViewModel, paddingValues = paddingValues)
    }
    
    EditWorkoutNameModal(labState = labState, labViewModel = labViewModel)
    EditProgramNameModal(labState = labState, labViewModel = labViewModel)
    DeleteProgramConfirmationDialog(labState = labState, labViewModel = labViewModel)
    DeleteWorkoutConfirmationDialog(labState = labState, labViewModel = labViewModel)
}

@Composable
fun WorkoutCardList(labState: LabState, labViewModel: LabViewModel, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.Center)
            .padding(paddingValues),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(labState.program?.workouts ?: listOf(), { it.id }) { workout ->
            WorkoutCard(
                state = labState,
                labViewModel = labViewModel,
                workout = workout
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun DraggableWorkoutList(labState: LabState, labViewModel: LabViewModel, paddingValues: PaddingValues) {
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            labViewModel.changePosition(to.index -1, from.index - 1)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(10.dp, 15.dp),
                    text = "Press, Hold & Drag to Reorder",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            }
        }
        items(labState.program?.workouts ?: listOf(), { it.id }) { workout ->
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
                        .clickable { labViewModel.saveReorder() },
                    text = "Confirm Reorder",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 15.dp, 0.dp)
                        .clickable { labViewModel.toggleReorderingScreen() },
                    text = "Cancel Reorder",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun WorkoutCard(modifier: Modifier = Modifier, state: LabState, labViewModel: LabViewModel, workout: ProgramDto.WorkoutDto) {
    OutlinedCard(
        modifier = modifier
            .fillMaxSize()
            .padding(5.dp),
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
                text = workout.name,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                modifier = Modifier.padding(15.dp, 0.dp, 0.dp, 0.dp)
            )
            WorkoutMenuDropdown(
                workout = workout,
                labState = state,
                labViewModel = labViewModel
            )
        }
        Divider(thickness = 12.dp, color = MaterialTheme.colorScheme.background)
        workout.lifts.forEach {
            Text(
                text = "${it.setCount} x ${it.lift.name}",
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
fun WorkoutMenuDropdown(workout: ProgramDto.WorkoutDto, labState: LabState, labViewModel: LabViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    MoreVertDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = isExpanded,
        onToggleExpansion = { isExpanded = !isExpanded }
    ) {
        DropdownMenuItem(
            text = { Text("Edit Name") },
            onClick = {
                isExpanded = false
                labViewModel.showEditWorkoutNameModal(workout)
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
                labViewModel.beginDeleteWorkout(workout)
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

@Composable
fun ProgramMenuDropdown(labViewModel: LabViewModel) {
    var isExpanded by remember { mutableStateOf(false) }

    MoreVertDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = isExpanded,
        onToggleExpansion = { isExpanded = !isExpanded }
    ) {
        DropdownMenuItem(
            text = { Text("Edit Name") },
            onClick = {
                isExpanded = false
                labViewModel.showEditProgramNameModal()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Add Workout") },
            onClick = { /*TODO*/ },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                isExpanded = false
                labViewModel.beginDeleteProgram()
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

@Composable
fun EditWorkoutNameModal(labState: LabState, labViewModel: LabViewModel) {
    if (labState.workoutOfEditNameModal != null) {
        TextFieldModal(
            header = "Rename ${labState.workoutOfEditNameModal.name}",
            initialTextFieldValue = labState.workoutOfEditNameModal.name,
            onConfirm = { labViewModel.updateWorkoutName(it) },
            onCancel = { labViewModel.collapseEditWorkoutNameModal() }
        )
    }
}

@Composable
fun EditProgramNameModal(labState: LabState, labViewModel: LabViewModel) {
    if (labState.isEditingProgramName) {
        TextFieldModal(
            header = "Rename ${labState.program?.name}",
            initialTextFieldValue = labState.program?.name ?: "",
            onConfirm = { labViewModel.updateProgramName(it) },
            onCancel = { labViewModel.collapseEditProgramNameModal() }
        )
    }
}

@Composable
fun DeleteProgramConfirmationDialog(labState: LabState, labViewModel: LabViewModel) {
    if (labState.isDeletingProgram) {
        ConfirmationModal(
            header = "Delete?",
            body = "Are you sure you want to delete ${labState.program?.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteProgram() },
            onCancel = { labViewModel.cancelDeleteProgram() }
        )
    }
}

@Composable
fun DeleteWorkoutConfirmationDialog(labState: LabState, labViewModel: LabViewModel) {
    if (labState.workoutToDelete != null) {
        ConfirmationModal(
            header = "Delete?",
            body = "Are you sure you want to delete ${labState.workoutToDelete.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteWorkout(labState.workoutToDelete) },
            onCancel = { labViewModel.cancelDeleteWorkout() }
        )
    }
}