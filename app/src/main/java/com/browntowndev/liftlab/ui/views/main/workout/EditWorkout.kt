package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import arrow.core.Either
import arrow.core.left
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.EditWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditWorkout(
    workoutLogEntryId: Long,
    paddingValues: PaddingValues,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
) {
    val editWorkoutViewModel: EditWorkoutViewModel = koinViewModel {
        parametersOf(workoutLogEntryId)
    }
    val state by editWorkoutViewModel.state.collectAsState()
    val durationState by editWorkoutViewModel.duration.collectAsState()

    LaunchedEffect(key1 = state.workout) {
        if (state.workout != null) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.TITLE,
                    "Edit ${state.workout!!.name}".left()
                )
            )
        }
    }

    if (state.workout != null) {
        WorkoutLog(
            paddingValues = paddingValues,
            visible = true,
            lifts = state.workout!!.lifts,
            duration = durationState,
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
            }
        )
    }
}