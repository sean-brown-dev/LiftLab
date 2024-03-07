package com.browntowndev.liftlab.ui.views.main.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.ui.viewmodels.PickerViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.views.composables.DeleteableOnSwipeLeft
import com.browntowndev.liftlab.ui.views.composables.LiftLabOutlinedTextField
import com.browntowndev.liftlab.ui.views.composables.RpeKeyboard
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Composable
fun WorkoutLog(
    paddingValues: PaddingValues,
    visible: Boolean,
    cancelWorkoutVisible: Boolean = true,
    noteVisible: Boolean = true,
    lifts: List<LoggingWorkoutLiftDto>,
    duration: String,
    onWeightChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, weight: Float?) -> Unit,
    onRepsChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, reps: Int?) -> Unit,
    onRpeSelected: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) -> Unit,
    onSetCompleted: (setType: SetType, progressionScheme: ProgressionScheme, liftPosition: Int, setPosition: Int,
                     myoRepSetPosition: Int?, liftId: Long, weight: Float, reps: Int, rpe: Float,
                     restTime: Long, restTimeEnabled: Boolean) -> Unit,
    undoCompleteSet: (liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) -> Unit,
    cancelWorkout: () -> Unit,
    onChangeRestTime: (workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) -> Unit,
    onReplaceLift: (workoutLiftId: Long, movementPattern: MovementPattern) -> Unit,
    onDeleteMyoRepSet: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int) -> Unit,
    onNoteChanged: (workoutLiftId: Long, note: String) -> Unit,
) {
    // Remember the myo rep set indices from the previous composition. Below they will
    // animate if they're not found in this set (they are new)
    var indicesOfExistingMyoRepSets by remember {
        mutableStateOf(
            lifts.flatMap { workoutLift ->
                workoutLift.sets
                    .filterIsInstance<LoggingMyoRepSetDto>()
                    .fastMap { set ->
                        "${workoutLift.id}-${set.myoRepSetPosition}"
                    }
            }.toSet()
        )
    }

    AnimatedVisibility(
        modifier = Modifier.animateContentSize(),
        visible = visible,
        enter = scaleIn(initialScale = .6f, animationSpec = tween(durationMillis = 250, easing = LinearEasing)) + fadeIn(),
        exit = scaleOut(targetScale = .6f, animationSpec = tween(durationMillis = 250, easing = LinearEasing)) + fadeOut(),
    ) {
        val lazyListState = rememberLazyListState()
        val pickerViewModel: PickerViewModel = koinViewModel()
        val pickerState by pickerViewModel.state.collectAsState()
        Box(contentAlignment = Alignment.BottomCenter) {
            var pickerSpacer: Dp by remember { mutableStateOf(0.dp) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = lazyListState,
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp, top = 20.dp, bottom = 15.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Duration:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = duration,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                items(lifts, key = { it.id }) { lift ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 5.dp),
                        shape = RectangleShape,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 16.dp,
                            pressedElevation = 0.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            val restTime = remember(lift.restTime) {
                                lift.restTime?.inWholeMilliseconds
                                    ?: SettingsManager.getSetting(
                                        SettingsManager.SettingNames.REST_TIME,
                                        SettingsManager.SettingNames.DEFAULT_REST_TIME,
                                    )
                            }
                            Row {
                                Text(
                                    modifier = Modifier.padding(start = 15.dp, top = 10.dp, bottom = 5.dp, end = 10.dp),
                                    text = lift.liftName,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                LiftDropdown(
                                    restTime = restTime.toDuration(DurationUnit.MILLISECONDS),
                                    restTimerEnabled = lift.restTimerEnabled,
                                    onChangeRestTime = { restTime, enabled ->
                                        onChangeRestTime(lift.id, restTime, enabled)
                                    },
                                    onReplaceLift = {
                                        onReplaceLift(lift.id, lift.liftMovementPattern)
                                    }
                                )
                            }
                            if (noteVisible) {
                                val localDensity = LocalDensity.current
                                var noteTextFieldHeight by remember { mutableStateOf(40.dp) }
                                LiftLabOutlinedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(noteTextFieldHeight)
                                        .padding(start = 15.dp, end = 10.dp),
                                    contentPadding = PaddingValues(start = 2.dp, top = 7.dp, bottom = 7.dp, end = 2.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        focusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                                    placeholder = {
                                        Text(
                                            text = remember { "Add note" },
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 18.sp,
                                        )
                                    },
                                    value = remember(lift.note) { lift.note ?: "" },
                                    shape = RoundedCornerShape(10.dp),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.lift_note),
                                            tint = MaterialTheme.colorScheme.outline,
                                        )
                                    },
                                    onValueChange = {
                                        onNoteChanged(lift.id, it)
                                        it
                                    },
                                    onRequiredHeightChanged = {
                                        localDensity.run {
                                            noteTextFieldHeight = it.toFloat().toDp() + 14.dp
                                        }
                                    },
                                )
                                Spacer(modifier = Modifier.height(15.dp))
                            }
                            LogHeaders()

                            lift.sets.fastForEach { set ->
                                DeleteableOnSwipeLeft(
                                    enabled = remember(set) { (set as? LoggingMyoRepSetDto)?.myoRepSetPosition != null },
                                    confirmationDialogHeader = "Delete Myorep Set?",
                                    confirmationDialogBody = "Confirm to delete the myorep set.",
                                    onDelete = {
                                        onDeleteMyoRepSet(lift.id, set.position, (set as LoggingMyoRepSetDto).myoRepSetPosition!!)
                                    },
                                    dismissContent = {
                                        val animateVisibility = remember(lift.sets.size) {
                                            set is LoggingMyoRepSetDto &&
                                                    !indicesOfExistingMyoRepSets.contains("${lift.id}-${set.myoRepSetPosition}")
                                        }

                                        LoggableSet(
                                            lazyListState = lazyListState,
                                            animateVisibility = animateVisibility,
                                            position = set.position,
                                            progressionScheme = lift.progressionScheme,
                                            setNumberLabel = set.setNumberLabel,
                                            weightRecommendation = set.weightRecommendation,
                                            rpeTarget = set.rpeTarget,
                                            complete = set.complete,
                                            completedWeight = set.completedWeight,
                                            completedReps = set.completedReps,
                                            completedRpe = set.completedRpe,
                                            previousSetResultLabel = set.previousSetResultLabel,
                                            repRangePlaceholder = set.repRangePlaceholder,
                                            onWeightChanged = {
                                                onWeightChanged(lift.id, set.position, (set as? LoggingMyoRepSetDto)?.myoRepSetPosition, it)
                                            },
                                            onRepsChanged = {
                                                onRepsChanged(lift.id, set.position, (set as? LoggingMyoRepSetDto)?.myoRepSetPosition, it)
                                            },
                                            toggleRpePicker = {
                                                if (it) {
                                                    pickerViewModel.showRpePicker(
                                                        workoutLiftId = lift.id,
                                                        setPosition = set.position,
                                                        myoRepSetPosition = (set as? LoggingMyoRepSetDto)?.myoRepSetPosition,
                                                    )
                                                } else {
                                                    pickerViewModel.hideRpePicker()
                                                }
                                            },
                                            onCompleted = { weight, reps, rpe ->
                                                val setType = when (set) {
                                                    is LoggingStandardSetDto -> SetType.STANDARD
                                                    is LoggingDropSetDto -> SetType.DROP_SET
                                                    is LoggingMyoRepSetDto -> SetType.MYOREP
                                                    else -> throw Exception("${set::class.simpleName} is not defined.")
                                                }
                                                onSetCompleted(
                                                    setType,
                                                    lift.progressionScheme,
                                                    lift.position,
                                                    set.position,
                                                    (set as? LoggingMyoRepSetDto)?.myoRepSetPosition,
                                                    lift.liftId,
                                                    weight,
                                                    reps,
                                                    rpe,
                                                    restTime,
                                                    lift.restTimerEnabled,
                                                )
                                                pickerViewModel.hideRpePicker()
                                            },
                                            onUndoCompletion = {
                                                undoCompleteSet(
                                                    lift.position,
                                                    set.position,
                                                    (set as? LoggingMyoRepSetDto)?.myoRepSetPosition,
                                                )
                                            },
                                            onAddSpacer = {
                                                pickerSpacer = it
                                            }
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                item {
                    if (cancelWorkoutVisible) {
                        TextButton(
                            modifier = Modifier.padding(20.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            onClick = cancelWorkout
                        ) {
                            Text(
                                text = "Cancel Workout",
                                fontSize = 18.sp,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Spacer(modifier = Modifier.height(pickerSpacer))
                    LaunchedEffect(pickerSpacer) {
                        if (pickerSpacer > 0.dp) {
                            lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                        }
                    }
                }
            }
            RpeKeyboard(
                visible = pickerState.type == PickerType.Rpe,
                onRpeSelected = {
                    onRpeSelected(pickerState.workoutLiftId!!, pickerState.setPosition!!, pickerState.myoRepSetPosition, it)
                },
                onClosed = {
                    pickerViewModel.hideRpePicker()
                    pickerSpacer = 0.dp
                },
            )
            BackHandler(pickerState.type == PickerType.Rpe) {
                pickerViewModel.hideRpePicker()
                pickerSpacer = 0.dp
            }
        }
    }

    // After everything is rendered update this map to include any new
    // myo rep sets that were added so they don't animate next time they
    // come into view
    LaunchedEffect(lifts) {
        indicesOfExistingMyoRepSets = lifts.flatMap { workoutLift ->
            workoutLift.sets
                .filterIsInstance<LoggingMyoRepSetDto>()
                .fastMap { set ->
                    "${workoutLift.id}-${(set as? LoggingMyoRepSetDto)?.myoRepSetPosition}"
                }
        }.toSet()
    }
}
