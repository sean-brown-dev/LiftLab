package com.browntowndev.liftlab.ui.views.main.workout

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.TimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen.Companion.REST_TIMER
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun Workout(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
) {
    var restTimerRestarted by remember { mutableStateOf(false) }

    val timerViewModel: TimerViewModel = koinViewModel()
    val workoutViewModel: WorkoutViewModel = koinViewModel()
    val state by workoutViewModel.state.collectAsState()
    val timerState by timerViewModel.state.collectAsState()

    LaunchedEffect(state.restTimerStartedAt) {
        if (state.restTimerStartedAt != null && !restTimerRestarted) {
            val restTimeRemaining = state.restTime - (Utils.getCurrentDate().time - state.restTimerStartedAt!!.time)
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(REST_TIMER, Triple(state.restTime, restTimeRemaining, true).right())
            )
            restTimerRestarted = true
            Log.d(Log.DEBUG.toString(), "restarted rest timer.")
        } else if (state.restTime == 0L && state.restTimerStartedAt == null) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(REST_TIMER, Triple(0L, 0L, false).right())
            )
        }
    }

    workoutViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = workoutViewModel)

    LaunchedEffect(key1 = state.workout != null, key2 = state.workoutLogVisible) {
        if (state.workout != null) {
            if (!state.workoutLogVisible) {
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(
                        Screen.TITLE,
                        state.workout!!.name.left()
                    )
                )
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(
                        Screen.SUBTITLE,
                        ("Mesocycle: ${state.programMetadata!!.currentMesocycle + 1} " +
                                "Microcycle: ${state.programMetadata!!.currentMicrocycle + 1}").left()
                    )
                )
            } else {
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(
                        Screen.TITLE,
                        "".left()
                    )
                )
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(
                        Screen.SUBTITLE,
                        "".left()
                    )
                )
            }

            setTopAppBarCollapsed(state.workoutLogVisible)
            setBottomNavBarVisibility(!state.workoutLogVisible)
            setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, state.workoutLogVisible)
            setTopAppBarControlVisibility(REST_TIMER, state.workoutLogVisible)

            if (!state.inProgress) {
                timerViewModel.stop()
            }
        }
    }

    LaunchedEffect(key1 = state.startTime) {
        if (state.startTime != null) {
            timerViewModel.startFrom(state.startTime!!) // This is smart and won't restart from 0
        }
    }

    if (state.workout != null) {
        WorkoutPreview(
            paddingValues = paddingValues,
            visible = !state.workoutLogVisible,
            workoutInProgress = state.inProgress,
            workoutName = state.workout!!.name,
            timeInProgress = timerState.time,
            lifts = state.workout!!.lifts,
            volumeTypes = state.volumeTypes,
            startWorkout = { workoutViewModel.startWorkout() },
            showWorkoutLog = { workoutViewModel.setWorkoutLogVisibility(true) }
        )
        WorkoutLog(
            paddingValues = paddingValues,
            visible = state.workoutLogVisible,
            lifts = state.workout!!.lifts,
            duration = timerState.time,
            onSetCompleted = { setType, progressionScheme, position, myoRepSetPosition, liftId, weight, reps, rpe, restTime ->
                workoutViewModel.completeSet(
                    restTime = restTime,
                    result = when (setType) {
                        SetType.STANDARD,
                        SetType.DROP_SET -> {
                            if (progressionScheme != ProgressionScheme.LINEAR_PROGRESSION) {
                                StandardSetResultDto(
                                    workoutId = state.workout!!.id,
                                    liftId = liftId,
                                    mesoCycle = state.programMetadata!!.currentMesocycle,
                                    microCycle = state.programMetadata!!.currentMicrocycle,
                                    setPosition = position,
                                    weight = weight,
                                    reps = reps,
                                    rpe = rpe,
                                )
                            } else {
                                // LP can only be standard lift, so no myo
                                LinearProgressionSetResultDto(
                                    workoutId = state.workout!!.id,
                                    liftId = liftId,
                                    mesoCycle = state.programMetadata!!.currentMesocycle,
                                    microCycle = state.programMetadata!!.currentMicrocycle,
                                    setPosition = position,
                                    weight = weight,
                                    reps = reps,
                                    rpe = rpe,
                                    missedLpGoals = 0, // assigned on completion
                                )
                            }
                        }

                        SetType.MYOREP ->
                            MyoRepSetResultDto(
                                workoutId = state.workout!!.id,
                                liftId = liftId,
                                mesoCycle = state.programMetadata!!.currentMesocycle,
                                microCycle = state.programMetadata!!.currentMicrocycle,
                                setPosition = position,
                                weight = weight,
                                reps = reps,
                                rpe = rpe,
                                myoRepSetPosition = myoRepSetPosition,
                            )
                    }
                )
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(REST_TIMER, Triple(restTime, restTime, true).right())
                )
            },
            undoCompleteSet = { liftId, setPosition, myoRepSetPosition ->
                workoutViewModel.undoSetCompletion(
                    liftId = liftId,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition
                )
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(REST_TIMER, Triple(0L, 0L, false).right())
                )
            },
            cancelWorkout = {
                workoutViewModel.cancelWorkout()
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(REST_TIMER, Triple(0L, 0L, false).right())
                )
            },
            onRpeSelected = { workoutLiftId, setPosition, myoRepSetPosition, newRpe ->
                workoutViewModel.setRpe(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    newRpe = newRpe,
                    myoRepSetPosition = myoRepSetPosition,
                )
            }
        )
    }
}
