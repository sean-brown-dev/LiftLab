package com.browntowndev.liftlab.ui.views.main.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import arrow.core.Either
import arrow.core.left
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.EditWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditWorkout(
    workoutLogEntryId: Long,
    paddingValues: PaddingValues,
    screenId: String?,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val editWorkoutViewModel: EditWorkoutViewModel = koinViewModel {
        parametersOf(workoutLogEntryId, onNavigateBack)
    }
    val workoutState by editWorkoutViewModel.workoutState.collectAsState()
    val editWorkoutState by editWorkoutViewModel.editWorkoutState.collectAsState()

    editWorkoutViewModel.registerEventBus()
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = editWorkoutViewModel)

    LaunchedEffect(key1 = workoutState.workout) {
        if (workoutState.workout != null) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.TITLE,
                    "Edit ${workoutState.workout!!.name}".left()
                )
            )
        }
    }

    val coroutineScope = rememberCoroutineScope()
    BackHandler(true) {
        coroutineScope.launch {
            editWorkoutViewModel.updateLinearProgressionFailures()
        }
        onNavigateBack()
    }

    if (workoutState.workout != null) {
        WorkoutLog(
            paddingValues = paddingValues,
            visible = true,
            cancelWorkoutVisible = false,
            noteVisible = false,
            lifts = workoutState.workout!!.lifts,
            duration = editWorkoutState.duration,
            onWeightChanged = { workoutLiftId, setPosition, myoRepSetPosition, weight ->
                editWorkoutViewModel.setWeight(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    newWeight = weight,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onRepsChanged = { workoutLiftId, setPosition, myoRepSetPosition, reps ->
                editWorkoutViewModel.setReps(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    newReps = reps,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onRpeSelected = { workoutLiftId, setPosition, myoRepSetPosition, newRpe ->
                editWorkoutViewModel.setRpe(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    newRpe = newRpe,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onSetCompleted = { setType, progressionScheme, liftPosition, setPosition, myoRepSetPosition, liftId, weight, reps, rpe, restTime, restTimerEnabled ->
                editWorkoutViewModel.completeSet(
                    restTime = restTime,
                    restTimerEnabled = restTimerEnabled,
                    result = editWorkoutViewModel.buildSetResult(
                        liftId = liftId,
                        setType = setType,
                        progressionScheme = progressionScheme,
                        liftPosition = liftPosition,
                        setPosition = setPosition,
                        myoRepSetPosition = myoRepSetPosition,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                    )
                )
            },
            undoCompleteSet = { liftPosition, setPosition, myoRepSetPosition ->
                editWorkoutViewModel.undoSetCompletion(
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition
                )
            },
            cancelWorkout = { },
            onChangeRestTime = { _, _, _ -> },
            onDeleteMyoRepSet = { workoutLiftId, setPosition, myoRepSetPosition ->
                editWorkoutViewModel.deleteMyoRepSet(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onNoteChanged = { _, _ -> },
            onReplaceLift = { _, _ -> },
            onReorderLiftsClicked = { },
        )
    }
}