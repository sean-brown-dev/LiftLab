package com.browntowndev.liftlab.ui.views.main.workout

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.runOnCompletion
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.ui.composables.ConfirmationDialog
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.ReorderableLazyColumn
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.TimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen.Companion.BACK_NAVIGATION_ICON
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen.Companion.REST_TIMER
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Workout(
    paddingValues: PaddingValues,
    screenId: String?,
    showLog: Boolean,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    onNavigateToWorkoutHistory: () -> Unit,
    onNavigateToLiftLibrary: (route: String) -> Unit,
) {
    val liftLevelDeloadsEnabled = remember {
        SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
    }
    var restTimerRestarted by remember { mutableStateOf(false) }

    val timerViewModel: TimerViewModel = koinViewModel()
    val workoutViewModel: WorkoutViewModel = koinViewModel {
        parametersOf(
            onNavigateToWorkoutHistory,
            {
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(REST_TIMER, Triple(0L, 0L, false).right())
                )
            },
            liftLevelDeloadsEnabled,
        )
    }
    val state by workoutViewModel.workoutState.collectAsState()
    val timerState by timerViewModel.state.collectAsState()

    LaunchedEffect(key1 = showLog) {
        if (showLog) {
            workoutViewModel.setWorkoutLogVisibility(true)
        }
    }

    LaunchedEffect(state.restTimerStartedAt) {
            if (state.restTimerStartedAt != null && !restTimerRestarted) {
            val restTimeRemaining = state.restTime - (getCurrentDate().time - state.restTimerStartedAt!!.time)
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
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = workoutViewModel)

    LaunchedEffect(key1 = state.workout, key2 = state.workoutLogVisible, key3 = state.initialized) {
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
        } else if (state.initialized) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.TITLE,
                    "Workout".left()
                )
            )
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.SUBTITLE,
                    "".left()
                )
            )
            setBottomNavBarVisibility(true)
        }
    }

    LaunchedEffect(key1 = state.startTime) {
        if (state.startTime != null) {
            timerViewModel.startFrom(state.startTime!!) // This is smart and won't restart from 0
        }
    }

    LaunchedEffect(key1 = state.isCompletionSummaryVisible, key2 = state.workoutLogVisible) {
        setTopAppBarControlVisibility(REST_TIMER, !state.isCompletionSummaryVisible && state.workoutLogVisible)

        // Changes it from down chevron to back arrow, also hides history icon
        setTopAppBarControlVisibility(BACK_NAVIGATION_ICON, state.isCompletionSummaryVisible)
    }

    if (state.workout != null) {
        val promptOnDeload = remember {
            SettingsManager.getSetting(PROMPT_FOR_DELOAD_WEEK, DEFAULT_PROMPT_FOR_DELOAD_WEEK)
        }
        WorkoutPreview(
            paddingValues = paddingValues,
            visible = !state.workoutLogVisible && !state.isReordering && !state.isCompletionSummaryVisible,
            workoutInProgress = state.inProgress,
            workoutName = state.workout!!.name,
            timeInProgress = timerState.time,
            lifts = state.workout!!.lifts,
            combinedVolumeTypes = state.combinedVolumeTypes,
            primaryVolumeTypes = state.primaryVolumeTypes,
            secondaryVolumeTypes = state.secondaryVolumeTypes,
            startWorkout = {
                if (promptOnDeload) {
                    workoutViewModel.showDeloadPromptOrStartWorkout()
                } else {
                    workoutViewModel.startWorkout()
                }
            },
            showWorkoutLog = { workoutViewModel.setWorkoutLogVisibility(true) }
        )
        WorkoutLog(
            paddingValues = paddingValues,
            visible = state.workoutLogVisible && !state.isReordering && !state.isCompletionSummaryVisible,
            lifts = state.workout!!.lifts,
            duration = timerState.time,
            onWeightChanged = { workoutLiftId, setPosition, myoRepSetPosition, weight ->
                workoutViewModel.setWeight(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    newWeight = weight,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onRepsChanged = { workoutLiftId, setPosition, myoRepSetPosition, reps ->
                workoutViewModel.setReps(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    newReps = reps,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onRpeSelected = { workoutLiftId, setPosition, myoRepSetPosition, newRpe ->
                workoutViewModel.setRpe(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    newRpe = newRpe,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onSetCompleted = { setType, progressionScheme, liftPosition, setPosition, myoRepSetPosition, liftId, weight, reps, rpe, restTime, restTimerEnabled ->
                val hasDropSetAfter = state.workout!!.lifts[liftPosition].sets.fastAny { set ->
                    set.position == (setPosition + 1) &&
                            set is LoggingDropSetDto
                }
                val startRestTimer =
                    !hasDropSetAfter && setType != SetType.MYOREP && restTimerEnabled

                workoutViewModel.completeSet(
                    restTime = restTime,
                    restTimerEnabled = startRestTimer,
                    result = workoutViewModel.buildSetResult(
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
                ).runOnCompletion {
                    if (startRestTimer || state.completedMyoRepSets) {
                        if (state.completedMyoRepSets) {
                            workoutViewModel.saveRestTimerInProgress(restTime)
                            workoutViewModel.resetMyoRepSetsCompleted()
                        }

                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                REST_TIMER,
                                Triple(restTime, restTime, true).right()
                            )
                        )
                    }
                }
            },
            onUndoSetCompletion = { liftPosition, setPosition, myoRepSetPosition ->
                workoutViewModel.undoSetCompletion(
                    liftPosition = liftPosition,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition
                )
            },
            cancelWorkout = {
                if (state.inProgressWorkout?.completedSets?.isNotEmpty() == true)
                    workoutViewModel.toggleConfirmCancelWorkoutModal()
                else
                    workoutViewModel.cancelWorkout()
            },
            onChangeRestTime = { workoutLiftId, newRestTime, enabled ->
                workoutViewModel.updateRestTime(
                    workoutLiftId = workoutLiftId,
                    newRestTime = newRestTime,
                    enabled = enabled,
                )
            },
            onReplaceLift = { workoutLiftId, movementPattern ->
                val liftLibraryRoute = LiftLibraryScreen.navigation.route
                    .replace("{callerRoute}", WorkoutScreen.navigation.route)
                    .replace("{workoutId}", state.workout!!.id.toString())
                    .replace("{workoutLiftId}", workoutLiftId.toString())
                    .replace(
                        "{movementPattern}",
                        movementPattern.displayName()
                    )

                onNavigateToLiftLibrary(liftLibraryRoute)
            },
            onDeleteMyoRepSet = { workoutLiftId, setPosition, myoRepSetPosition ->
                workoutViewModel.deleteMyoRepSet(
                    workoutLiftId = workoutLiftId,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition,
                )
            },
            onNoteChanged = { workoutLiftId, note ->
              workoutViewModel.updateNote(workoutLiftId = workoutLiftId, note = note)
            },
            onReorderLiftsClicked = {
                workoutViewModel.toggleReorderLifts()
            },
        )
        if (state.isReordering) {
            ReorderableLazyColumn(
                paddingValues = paddingValues,
                items = remember(state.workout!!.lifts) { state.workout!!.lifts.fastMap { ReorderableListItem(it.liftName, it.id) } },
                saveReorder = { workoutViewModel.reorderLifts(it) },
                cancelReorder = { workoutViewModel.toggleReorderLifts() }
            )
        }
        if (state.isCompletionSummaryVisible && state.workoutCompletionSummary != null) {
            CompletionSummary(
                paddingValues = paddingValues,
                workoutCompletionSummary = state.workoutCompletionSummary!!,
                startTime = state.inProgressWorkout!!.startTime,
                onCancel = workoutViewModel::toggleCompletionSummary
            )
        }
        if (state.isConfirmCancelWorkoutDialogShown) {
            ConfirmationDialog(
                header = stringResource(R.string.confirm_cancellation),
                textAboveContent = stringResource(R.string.cancel_workout_confirm_body),
                onConfirm = {
                    workoutViewModel.cancelWorkout()
                    mutateTopAppBarControlValue(
                        AppBarMutateControlRequest(REST_TIMER, Triple(0L, 0L, false).right())
                    )
                },
                onCancel = { workoutViewModel.toggleConfirmCancelWorkoutModal() })
        }
        if (state.isDeloadPromptDialogShown) {
            ConfirmationDialog(
                header = "Take Deload Microcycle?",
                textAboveContent = "Do you want to take a deload microcycle or skip it? " +
                        "Consider the following questions as well as your sleep, " +
                        "diet and stress levels. ",
                textAboveContentPadding = PaddingValues(),
                textAboveContentAlignment = TextAlign.Left,
                confirmButtonText = "Take Deload",
                cancelButtonText = "Skip",
                onConfirm = workoutViewModel::startWorkout,
                onDismiss = workoutViewModel::toggleDeloadPrompt,
                onCancel = {
                    workoutViewModel.skipDeloadMicrocycle()
                    workoutViewModel.startWorkout()
                },
            ) {
                Column (
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "• Dreading the gym?\n" +
                                "• Progress stalled?\n" +
                                "• Fatigued?\n" +
                                "• Aches and pains?",
                        fontSize = 16.sp,
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 20.dp),
                        text = "If you answered yes to two or more, and everything else is in line, you should deload.",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
