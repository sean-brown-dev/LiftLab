package com.browntowndev.liftlab.ui.views.main.lab

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.DELOAD_WEEK_OPTIONS
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.ui.composables.ConfirmationDialog
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.LiftLabDialog
import com.browntowndev.liftlab.ui.composables.NumberPickerSpinner
import com.browntowndev.liftlab.ui.composables.ReorderableLazyColumn
import com.browntowndev.liftlab.ui.composables.TextFieldDialog
import com.browntowndev.liftlab.ui.composables.VolumeChipBottomSheet
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import org.koin.androidx.compose.koinViewModel

@ExperimentalFoundationApi
@Composable
fun Lab(
    paddingValues: PaddingValues,
    screenId: String?,
    onNavigateToWorkoutBuilder: (workoutId: Long) -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit,
) {
    val labViewModel: LabViewModel = koinViewModel()
    val state by labViewModel.state.collectAsState()
    val isLiftSpecificDeloadingEnabled = remember {
        SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
    }

    LaunchedEffect(state.program) {
        if(state.program != null) {
            if (!state.isManagingPrograms) {
                mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, state.originalProgramName))
            }
            setTopAppBarControlVisibility(LabScreen.RENAME_PROGRAM_ICON, true)
            setTopAppBarControlVisibility(LabScreen.DELETE_PROGRAM_ICON, true)
            setTopAppBarControlVisibility(LabScreen.CREATE_NEW_WORKOUT_ICON, true)
            setTopAppBarControlVisibility(LabScreen.REORDER_WORKOUTS_ICON, state.program!!.workouts.size > 1)
            setTopAppBarControlVisibility(LabScreen.MANAGE_PROGRAMS_ICON, true)

            if (!isLiftSpecificDeloadingEnabled) {
                setTopAppBarControlVisibility(LabScreen.DELOAD_WEEK_ICON, true)
                mutateTopAppBarControlValue(AppBarMutateControlRequest(LabScreen.DELOAD_WEEK_ICON, state.program!!.deloadWeek.toString()))
            }
        } else {
            if (!state.isManagingPrograms) {
                mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, ""))
            }
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

    LaunchedEffect(key1 = state.isManagingPrograms) {
        if (state.isManagingPrograms) {
            mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, "Program Management"))
        } else {
            mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, state.originalProgramName))
        }
        setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, state.isManagingPrograms)
        setTopAppBarControlVisibility(Screen.OVERFLOW_MENU_ICON, !state.isManagingPrograms)
    }

    labViewModel.registerEventBus()
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = labViewModel)

    if (state.isReordering) {
        ReorderableLazyColumn(
            paddingValues = paddingValues,
            items = remember(state.program!!.workouts) { state.program!!.workouts.fastMap { ReorderableListItem(it.name, it.id) } },
            saveReorder = { labViewModel.saveReorder(it) },
            cancelReorder = { labViewModel.toggleReorderingScreen() }
        )
    }
    else if (state.isManagingPrograms) {
        ProgramManager(
            paddingValues = paddingValues,
            programs = state.allPrograms,
            onCreateProgram = { labViewModel.toggleCreateProgramModal() },
            onSetProgramAsActive = { labViewModel.setProgramAsActive(it) },
            onDeleteProgram = { labViewModel.beginDeleteProgram(it) },
            onNavigateBack = { labViewModel.toggleManageProgramsScreen() })
    }
    else if (state.program?.workouts?.isEmpty() == false) {
        VolumeChipBottomSheet(
            placeAboveBottomNavBar = true,
            title = "Program Volume",
            combinedVolumeChipLabels = state.combinedVolumeTypes,
            primaryVolumeChipLabels = state.primaryVolumeTypes,
            secondaryVolumeChipLabels = state.secondaryVolumeTypes,
        ) {
            WorkoutCardList(
                paddingValues = paddingValues,
                workouts = state.program!!.workouts,
                showEditWorkoutNameModal = { workout ->
                    labViewModel.showEditWorkoutNameModal(
                        workout.id,
                        workout.name
                    )
                },
                beginDeleteWorkout = { labViewModel.beginDeleteWorkout(it) },
                onNavigateToWorkoutBuilder = onNavigateToWorkoutBuilder,
            )
        }
    }

    if (state.workoutIdToRename != null && state.originalWorkoutName != null) {
        TextFieldDialog(
            header = "Rename ${state.originalWorkoutName}",
            initialTextFieldValue = state.originalWorkoutName!!,
            onConfirm = { labViewModel.updateWorkoutName(state.workoutIdToRename!!, it) },
            onCancel = { labViewModel.collapseEditWorkoutNameModal() },
        )
    }

    if (state.isEditingProgramName && state.program != null) {
        TextFieldDialog(
            header = "Rename ${state.program!!.name}",
            initialTextFieldValue = state.program!!.name,
            onConfirm = { labViewModel.updateProgramName(it) },
            onCancel = { labViewModel.collapseEditProgramNameModal() }
        )
    }
    if (state.isEditingDeloadWeek && state.program != null) {
        LiftLabDialog(
            isVisible = true,
            header = "Change Deload Week",
            onDismiss = labViewModel::toggleEditDeloadWeek
        ) {
            NumberPickerSpinner(
                options = DELOAD_WEEK_OPTIONS,
                initialValue = state.program!!.deloadWeek.toFloat(),
                onChanged = {
                    labViewModel.updateDeloadWeek(it.toInt())
                },
            )
        }
    }

    if (state.isCreatingProgram) {
        val subtext = if(state.program != null && !state.isManagingPrograms) {
            "Creating a new program will archive the existing one. " +
                    "It can be restored or deleted from the Manage Programs menu."
        } else ""

        TextFieldDialog(
            header = "Create New Program",
            textAboveTextField = subtext,
            placeholder = "Name",
            initialTextFieldValue = "",
            onConfirm = { labViewModel.createProgram(it) },
            onCancel = { labViewModel.toggleCreateProgramModal() }
        )
    }

    if (state.isDeletingProgram && state.idOfProgramToDelete != null) {
        ConfirmationDialog(
            header = "Delete?",
            textAboveContent = "Are you sure you want to delete ${state.nameOfProgramToDelete}? This cannot be undone.",
            onConfirm = { labViewModel.deleteProgram(state.idOfProgramToDelete!!) },
            onCancel = { labViewModel.cancelDeleteProgram() }
        )
    }

    if (state.workoutToDelete != null) {
        ConfirmationDialog(
            header = "Delete?",
            textAboveContent = "Are you sure you want to delete ${state.workoutToDelete!!.name}? This cannot be undone.",
            onConfirm = { labViewModel.deleteWorkout(state.workoutToDelete!!) },
            onCancel = { labViewModel.cancelDeleteWorkout() }
        )
    }
}
