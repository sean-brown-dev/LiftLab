package com.browntowndev.liftlab.ui.views.main.lab

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastMap
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.composables.ConfirmationModal
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.composables.ReorderableLazyColumn
import com.browntowndev.liftlab.ui.views.composables.TextFieldModal
import com.browntowndev.liftlab.ui.views.composables.VolumeChipBottomSheet
import org.koin.androidx.compose.koinViewModel

@ExperimentalFoundationApi
@Composable
fun Lab(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit,
) {
    val labViewModel: LabViewModel = koinViewModel()
    val state by labViewModel.state.collectAsState()

    LaunchedEffect(state.program) {
        if(state.program != null) {
            mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, state.originalProgramName))
            mutateTopAppBarControlValue(AppBarMutateControlRequest(LabScreen.DELOAD_WEEK_ICON, state.program!!.deloadWeek.toString()))
            setTopAppBarControlVisibility(LabScreen.RENAME_PROGRAM_ICON, true)
            setTopAppBarControlVisibility(LabScreen.DELETE_PROGRAM_ICON, true)
            setTopAppBarControlVisibility(LabScreen.CREATE_NEW_WORKOUT_ICON, true)
            setTopAppBarControlVisibility(LabScreen.REORDER_WORKOUTS_ICON, state.program!!.workouts.size > 1)
            setTopAppBarControlVisibility(LabScreen.DELOAD_WEEK_ICON, true)
        } else {
            mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, ""))
            setTopAppBarControlVisibility(LabScreen.RENAME_PROGRAM_ICON, false)
            setTopAppBarControlVisibility(LabScreen.DELETE_PROGRAM_ICON, false)
            setTopAppBarControlVisibility(LabScreen.CREATE_NEW_WORKOUT_ICON, false)
            setTopAppBarControlVisibility(LabScreen.REORDER_WORKOUTS_ICON, false)
            setTopAppBarControlVisibility(LabScreen.DELOAD_WEEK_ICON, false)
        }
    }

    LaunchedEffect(key1 = state.isReordering) {
        setTopAppBarCollapsed(state.isReordering)
        setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, state.isReordering)
        setTopAppBarControlVisibility(Screen.OVERFLOW_MENU_ICON, !state.isReordering)
    }

    labViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = labViewModel)

    if (!state.isReordering) {
        if (state.program?.workouts?.isEmpty() == false) {
            VolumeChipBottomSheet(
                placeAboveBottomNavBar = true,
                title = "Program Volume",
                state.volumeTypes
            ) {
                WorkoutCardList(
                    paddingValues = paddingValues,
                    workouts = state.program!!.workouts,
                    showEditWorkoutNameModal = { workout -> labViewModel.showEditWorkoutNameModal(workout.id, workout.name) },
                    beginDeleteWorkout = { labViewModel.beginDeleteWorkout(it) },
                    navigationController = navHostController
                )
            }
        }
    }
    else {
        ReorderableLazyColumn(
            paddingValues = paddingValues,
            items = state.program!!.workouts.fastMap { ReorderableListItem(it.name, it.id) },
            saveReorder = { labViewModel.saveReorder(it) },
            cancelReorder = { labViewModel.toggleReorderingScreen() }
        )
    }

    if (state.workoutIdToRename != null && state.originalWorkoutName != null) {
        TextFieldModal(
            header = "Rename ${state.originalWorkoutName}",
            initialTextFieldValue = state.originalWorkoutName!!,
            onConfirm = { labViewModel.updateWorkoutName(state.workoutIdToRename!!, it) },
            onCancel = { labViewModel.collapseEditWorkoutNameModal() },
        )
    }

    if (state.isEditingProgramName && state.program != null) {
        TextFieldModal(
            header = "Rename ${state.program!!.name}",
            initialTextFieldValue = state.program!!.name,
            onConfirm = { labViewModel.updateProgramName(it) },
            onCancel = { labViewModel.collapseEditProgramNameModal() }
        )
    }

    if (state.isEditingDeloadWeek && state.program != null) {
        TextFieldModal(
            header = "Edit Deload Week",
            initialTextFieldValue = state.program!!.deloadWeek,
            onConfirm = { labViewModel.updateDeloadWeek(it) },
            onCancel = { labViewModel.toggleEditDeloadWeek() }
        )
    }

    if (state.isCreatingProgram) {
        val subtext = if(state.program != null) {
            "Creating a new program will archive the existing one." +
                    "It can be restored or deleted from the Settings menu."
        } else ""

        TextFieldModal(
            header = "Create New Program",
            subtext = subtext,
            placeholder = "Name",
            initialTextFieldValue = "",
            onConfirm = { labViewModel.createProgram(it) },
            onCancel = { labViewModel.toggleCreateProgramModal() }
        )
    }

    if (state.isDeletingProgram && state.program != null) {
        ConfirmationModal(
            header = "Delete?",
            body = "Are you sure you want to delete ${state.program!!.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteProgram() },
            onCancel = { labViewModel.cancelDeleteProgram() }
        )
    }

    if (state.workoutToDelete != null) {
        ConfirmationModal(
            header = "Delete?",
            body = "Are you sure you want to delete ${state.workoutToDelete!!.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteWorkout(state.workoutToDelete!!) },
            onCancel = { labViewModel.cancelDeleteWorkout() }
        )
    }
}
