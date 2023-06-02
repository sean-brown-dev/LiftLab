package com.browntowndev.liftlab.ui.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.LabState
import com.browntowndev.liftlab.ui.views.utils.ConfirmationModal
import com.browntowndev.liftlab.ui.views.utils.MoreVertDropdown
import com.browntowndev.liftlab.ui.views.utils.TextFieldModal
import org.koin.androidx.compose.getViewModel

@Composable
fun Lab(
    paddingValues: PaddingValues,
    labViewModel: LabViewModel = getViewModel()
) {
    val state by labViewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.Center)
            .padding(paddingValues),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(state.programs) {program ->
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp),
                    text = program.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                )
                ProgramMenuDropdown(
                    program = program,
                    labState = state,
                    labViewModel = labViewModel
                )
            }

            program.workouts.forEach { workout ->
                WorkoutCard(
                    state = state,
                    labViewModel = labViewModel,
                    workout = workout
                )
            }
        }
    }

    EditWorkoutNameModal(labState = state, labViewModel = labViewModel)
    EditProgramNameModal(labState = state, labViewModel = labViewModel)
    DeleteProgramConfirmationDialog(labState = state, labViewModel = labViewModel)
    DeleteWorkoutConfirmationDialog(labState = state, labViewModel = labViewModel)
}

@Composable
fun WorkoutCard(state: LabState, labViewModel: LabViewModel, workout: ProgramDto.WorkoutDto) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp),
        shape = CardDefaults.shape,
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
fun ProgramMenuDropdown(program: ProgramDto, labState: LabState, labViewModel: LabViewModel) {
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
                labViewModel.showEditProgramNameModal(program)
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
                labViewModel.beginDeleteProgram(program)
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
    if (labState.programOfEditNameModal != null) {
        TextFieldModal(
            header = "Rename ${labState.programOfEditNameModal.name}",
            initialTextFieldValue = labState.programOfEditNameModal.name,
            onConfirm = { labViewModel.updateProgramName(it) },
            onCancel = { labViewModel.collapseEditProgramNameModal() }
        )
    }
}

@Composable
fun DeleteProgramConfirmationDialog(labState: LabState, labViewModel: LabViewModel) {
    if (labState.programToDelete != null) {
        ConfirmationModal(
            header = "Delete?",
            body = "Are you sure you want to delete ${labState.programToDelete.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteProgram(labState.programToDelete) },
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