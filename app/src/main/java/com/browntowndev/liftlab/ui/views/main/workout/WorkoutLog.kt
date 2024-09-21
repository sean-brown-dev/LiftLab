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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.ui.composables.RpeKeyboard
import com.browntowndev.liftlab.ui.viewmodels.PickerViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration


@Composable
fun WorkoutLog(
    paddingValues: PaddingValues,
    visible: Boolean,
    isEdit: Boolean = false,
    lifts: List<LoggingWorkoutLiftDto>,
    duration: String,
    onWeightChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, weight: Float?) -> Unit,
    onRepsChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, reps: Int?) -> Unit,
    onRpeSelected: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) -> Unit,
    onSetCompleted: (setType: SetType, progressionScheme: ProgressionScheme, liftPosition: Int, setPosition: Int,
                     myoRepSetPosition: Int?, liftId: Long, weight: Float, reps: Int, rpe: Float,
                     restTime: Long, restTimeEnabled: Boolean) -> Unit,
    onUndoSetCompletion: (liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) -> Unit,
    cancelWorkout: () -> Unit,
    onChangeRestTime: (workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) -> Unit,
    onReplaceLift: (workoutLiftId: Long, movementPattern: MovementPattern) -> Unit,
    onDeleteMyoRepSet: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int) -> Unit,
    onNoteChanged: (workoutLiftId: Long, note: String) -> Unit,
    onReorderLiftsClicked: () -> Unit,
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
                        if (!isEdit) {
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onReorderLiftsClicked) {
                                Icon(
                                    modifier = Modifier.size(28.dp),
                                    painter = painterResource(id = R.drawable.reorder_icon),
                                    contentDescription = stringResource(R.string.reorder_lifts),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                items(lifts, key = { it.id }) { lift ->
                    WorkoutLiftCard(
                        lift = lift,
                        isEdit = isEdit,
                        indicesOfExistingMyoRepSets = indicesOfExistingMyoRepSets,
                        lazyListState = lazyListState,
                        onUndoSetCompletion = onUndoSetCompletion,
                        onChangeRestTime = onChangeRestTime,
                        onWeightChanged = onWeightChanged,
                        onRepsChanged = onRepsChanged,
                        onReplaceLift = onReplaceLift,
                        onNoteChanged = onNoteChanged,
                        onSetCompleted = onSetCompleted,
                        onDeleteMyoRepSet = onDeleteMyoRepSet,
                        onHideRpePicker = {
                            pickerViewModel.hideRpePicker()
                        },
                        onShowRpePicker = { workoutLiftId, setPosition, myoRepSetPosition, currentRpe ->
                            pickerViewModel.showRpePicker(
                                workoutLiftId = workoutLiftId,
                                setPosition = setPosition,
                                myoRepSetPosition = myoRepSetPosition,
                                currentRpe = currentRpe,
                            )
                        },
                        onUpdatePickerSpacer = { padding ->
                            pickerSpacer = padding
                        }
                    )
                }
                item {
                    if (!isEdit) {
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
                selectedRpe = pickerState.currentRpe,
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
