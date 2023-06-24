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
import com.browntowndev.liftlab.ui.views.utils.ConfirmationModal
import com.browntowndev.liftlab.ui.views.utils.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.utils.ReorderableLazyColumn
import com.browntowndev.liftlab.ui.views.utils.TextFieldModal
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

    LaunchedEffect(labState.program) {
        if(labState.program != null) {
            mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, labState.originalProgramName))
            mutateTopAppBarControlValue(AppBarMutateControlRequest(LabScreen.DELOAD_WEEK_ICON, labState.program!!.deloadWeek.toString()))
            setTopAppBarControlVisibility(LabScreen.RENAME_PROGRAM_ICON, true)
            setTopAppBarControlVisibility(LabScreen.DELETE_PROGRAM_ICON, true)

            labViewModel.registerEventBus()
        }
    }

    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = labViewModel)

    if (!labState.isReordering) {
        setTopAppBarCollapsed(false)
        setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, false)
        setTopAppBarControlVisibility(Screen.OVERFLOW_MENU_ICON, true)

        if (labState.program?.workouts != null) {
            WorkoutCardList(
                workouts = labState.program!!.workouts,
                showEditWorkoutNameModal = { workout -> labViewModel.showEditWorkoutNameModal(workout.id, workout.name) },
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
            ReorderableLazyColumn(
                paddingValues = paddingValues,
                items = labState.program!!.workouts.fastMap { ReorderableListItem(it.name, it.id) },
                saveReorder = { labViewModel.saveReorder(it) },
                cancelReorder = { labViewModel.toggleReorderingScreen() }
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

    if (labState.isEditingDeloadWeek && labState.program != null) {
        TextFieldModal(
            header = "Edit Deload Week",
            initialTextFieldValue = labState.program!!.deloadWeek,
            onConfirm = { labViewModel.updateDeloadWeek(it) },
            onCancel = { labViewModel.toggleEditDeloadWeek() }
        )
    }

    if (labState.isCreatingProgram) {
        val subtext = if(labState.program != null) {
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
