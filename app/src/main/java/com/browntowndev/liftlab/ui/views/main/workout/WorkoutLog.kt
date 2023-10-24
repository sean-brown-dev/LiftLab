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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.ui.viewmodels.PickerViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.views.composables.DeleteableOnSwipeLeft
import com.browntowndev.liftlab.ui.views.composables.RpePicker
import org.koin.androidx.compose.getViewModel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Composable
fun WorkoutLog(
    paddingValues: PaddingValues,
    visible: Boolean,
    lifts: List<LoggingWorkoutLiftDto>,
    duration: String,
    onWeightChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, weight: Float?) -> Unit,
    onRepsChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, reps: Int?) -> Unit,
    onRpeSelected: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) -> Unit,
    onSetCompleted: (setType: SetType, progressionScheme: ProgressionScheme, liftPosition: Int, setPosition: Int,
                     myoRepSetPosition: Int?, liftId: Long, weight: Float, reps: Int, rpe: Float,
                     restTime: Long, restTimeEnabled: Boolean) -> Unit,
    undoCompleteSet: (liftId: Long, setPosition: Int, myoRepSetPosition: Int?) -> Unit,
    cancelWorkout: () -> Unit,
    onChangeRestTime: (workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) -> Unit,
    onDeleteMyoRepSet: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int) -> Unit,
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
        val pickerViewModel: PickerViewModel = getViewModel()
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
                                )
                            }
                            LogHeaders()

                            lift.sets.fastForEachIndexed { index, set ->
                                DeleteableOnSwipeLeft(
                                    enabled = remember(set) { set is LoggingMyoRepSetDto && (index == (lift.sets.size - 1)) },
                                    confirmationDialogHeader = "Delete Myorep Set?",
                                    confirmationDialogBody = "Confirm to delete the myorep set.",
                                    onDelete = {
                                        onDeleteMyoRepSet(lift.id, set.setPosition, (set as LoggingMyoRepSetDto).myoRepSetPosition!!)
                                    },
                                    dismissContent = {
                                        val animateVisibility = remember(lift.sets.size) {
                                            set is LoggingMyoRepSetDto &&
                                                    !indicesOfExistingMyoRepSets.contains("${lift.id}-${set.myoRepSetPosition}")
                                        }

                                        LoggableSet(
                                            lazyListState = lazyListState,
                                            animateVisibility = animateVisibility,
                                            position = set.setPosition,
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
                                                onWeightChanged(lift.id, set.setPosition, (set as? LoggingMyoRepSetDto)?.myoRepSetPosition, it)
                                            },
                                            onRepsChanged = {
                                                onRepsChanged(lift.id, set.setPosition, (set as? LoggingMyoRepSetDto)?.myoRepSetPosition, it)
                                            },
                                            toggleRpePicker = {
                                                if (it) {
                                                    pickerViewModel.showRpePicker(
                                                        workoutLiftId = lift.id,
                                                        setPosition = set.setPosition,
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
                                                    set.setPosition,
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
                                                    lift.liftId,
                                                    set.setPosition,
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

                    Spacer(modifier = Modifier.height(pickerSpacer))
                    LaunchedEffect(pickerSpacer) {
                        if (pickerSpacer > 0.dp) {
                            lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                        }
                    }
                }
            }
            RpePicker(
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
