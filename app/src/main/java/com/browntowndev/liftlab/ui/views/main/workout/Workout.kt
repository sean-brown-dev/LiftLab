package com.browntowndev.liftlab.ui.views.main.workout

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.PROMPT_FOR_DELOAD_WEEK
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.displayName
import com.browntowndev.liftlab.ui.composables.ConfirmationDialog
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.ReorderableLazyColumn
import com.browntowndev.liftlab.ui.composables.ShimmerSkeletonList
import com.browntowndev.liftlab.ui.composables.SnackbarProvider
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.models.controls.ReorderableListItem
import com.browntowndev.liftlab.ui.models.controls.Route
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingDropSetUiModel
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.Screen
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.WorkoutScreen.Companion.BACK_NAVIGATION_ICON
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.WorkoutScreen.Companion.REST_TIMER
import com.browntowndev.liftlab.ui.viewmodels.timer.DurationTimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.workout.WorkoutViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Workout(
    paddingValues: PaddingValues,
    screenId: String?,
    snackbarHostState: SnackbarHostState,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Triple<Long, Long, Boolean>>>) -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
    onNavigateToWorkoutHistory: () -> Unit,
    onNavigateToLiftLibrary: (route: Route.LiftLibrary) -> Unit,
) {
    val durationTimerViewModel: DurationTimerViewModel = koinViewModel()
    val workoutViewModel: WorkoutViewModel = koinViewModel {
        parametersOf(
            onNavigateToWorkoutHistory
        )
    }
    val state by workoutViewModel.workoutState.collectAsState()
    val timerState by durationTimerViewModel.state.collectAsState()

    workoutViewModel.registerEventBus()
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = workoutViewModel)
    SnackbarProvider(snackbarHostState, workoutViewModel.userMessages)

    LaunchedEffect(state.workout?.id, state.workoutLogVisible, state.initialized) {
        val logVisible = state.workoutLogVisible
        val workoutId = state.workout?.id
        val initialized = state.initialized

        // HOLD during global loading
        if (!initialized) return@LaunchedEffect

        // ---------- Controls (only when not holding) ----------
        setTopAppBarCollapsed(logVisible)
        setBottomNavBarVisibility(!logVisible)
        setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, logVisible)
        setTopAppBarControlVisibility(REST_TIMER, logVisible)
        if (!state.inProgress) durationTimerViewModel.stop()

        // ---------- Title / Subtitle (only when not holding) ----------
        when {
            // No workout, show default workout name
            workoutId == null -> {
                mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.TITLE, "Workout".left()))
                mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, "".left()))
                setBottomNavBarVisibility(true)
            }
            // Summary header when log hidden and we have a workout
            !logVisible -> {
                mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.TITLE, state.workout!!.name.left()))
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(
                        Screen.SUBTITLE,
                        ("Mesocycle: ${state.programMetadata!!.currentMesocycle + 1} " +
                                "Microcycle: ${state.programMetadata!!.currentMicrocycle + 1}").left()
                    )
                )
            }
            // Log visible → clear header
            else -> {
                mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.TITLE, "".left()))
                mutateTopAppBarControlValue(AppBarMutateControlRequest(Screen.SUBTITLE, "".left()))
            }
        }
    }

    LaunchedEffect(key1 = state.startTime, key2 = timerState.running) {
        if (state.startTime != null && !timerState.running) {
            durationTimerViewModel.startFrom(state.startTime!!) // This is smart and won't restart from 0
        }
    }

    LaunchedEffect(key1 = state.isCompletionSummaryVisible, key2 = state.workoutLogVisible) {
        setTopAppBarControlVisibility(REST_TIMER, !state.isCompletionSummaryVisible && state.workoutLogVisible)

        // Changes it from down chevron to back arrow, also hides history icon
        setTopAppBarControlVisibility(BACK_NAVIGATION_ICON, state.isCompletionSummaryVisible)
        if (state.isCompletionSummaryVisible) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.TITLE,
                    "Summary".left()
                )
            )
        } else if (state.workoutLogVisible) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.TITLE,
                    "".left()
                )
            )
        }
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
                            set is LoggingDropSetUiModel
                }
                val startRestTimer = !hasDropSetAfter && setType != SetType.MYOREP && restTimerEnabled

                try {
                    if (startRestTimer) {
                        mutateTopAppBarControlValue(
                            AppBarMutateControlRequest(
                                REST_TIMER,
                                Triple(restTime, restTime, true).right()
                            )
                        )
                    }
                    workoutViewModel.completeSet(
                        restTime = restTime,
                        restTimerEnabled = startRestTimer,
                        onBuildSetResult = {
                            workoutViewModel.buildSetResult(
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
                        }
                    ) {
                        if (startRestTimer) {
                            // On error, shut off the rest timer
                            mutateTopAppBarControlValue(
                                AppBarMutateControlRequest(
                                    REST_TIMER,
                                    Triple(0L, 0L, false).right()
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Workout", "Failed to complete set", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    workoutViewModel.emitUserMessage("Failed to complete set")
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
                if (state.completedSets.isNotEmpty())
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
                onNavigateToLiftLibrary(Route.LiftLibrary(
                    callerRouteId = Route.Workout.id,
                    workoutId = state.workout!!.id,
                    workoutLiftId = workoutLiftId,
                    movementPattern = movementPattern.displayName(),
                ))
            },
            onNoteChanged = { liftId, note ->
              workoutViewModel.updateNote(liftId = liftId, note = note)
            },
            onReorderLiftsClicked = {
                workoutViewModel.toggleReorderLifts()
            },
            onAddSet = {_ -> }
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
            val context = LocalContext.current
            CompletionSummary(
                paddingValues = paddingValues,
                workoutCompletionSummary = state.workoutCompletionSummary!!,
                startTime = state.inProgressWorkout!!.startTime,
                onShare = { workoutSummaryBitmap ->
                    workoutViewModel.shareWorkoutSummary(
                        context = context,
                        workoutSummaryBitmap = workoutSummaryBitmap)
                },
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
                onCancel = workoutViewModel::skipDeloadMicrocycleAndStartWorkout,
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
    } else if (state.workoutLogVisible) {
        ShimmerSkeletonList(
            modifier = Modifier.padding(paddingValues).padding(top = 85.dp)
        )
    }
}
