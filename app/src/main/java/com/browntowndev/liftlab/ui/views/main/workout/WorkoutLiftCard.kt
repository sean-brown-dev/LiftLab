package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.ui.composables.DeleteableOnSwipeLeft
import com.browntowndev.liftlab.ui.composables.LiftLabOutlinedTextField
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun WorkoutLiftCard(
    lift: LoggingWorkoutLiftDto,
    isEdit: Boolean,
    indicesOfExistingMyoRepSets: Set<String>,
    lazyListState: LazyListState,
    onUpdatePickerSpacer: (padding: Dp) -> Unit,
    onShowRpePicker: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?) -> Unit,
    onHideRpePicker: () -> Unit,
    onWeightChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, weight: Float?) -> Unit,
    onRepsChanged: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, reps: Int?) -> Unit,
    onSetCompleted: (setType: SetType, progressionScheme: ProgressionScheme, liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?, liftId: Long, weight: Float, reps: Int, rpe: Float, restTime: Long, restTimeEnabled: Boolean) -> Unit,
    onUndoSetCompletion: (liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?) -> Unit,
    onChangeRestTime: (workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) -> Unit,
    onReplaceLift: (workoutLiftId: Long, movementPattern: MovementPattern) -> Unit,
    onNoteChanged: (workoutLiftId: Long, note: String) -> Unit,
    onDeleteMyoRepSet: (workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 16.dp,
            pressedElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                    modifier = Modifier.padding(
                        start = 15.dp,
                        top = 10.dp,
                        bottom = 5.dp,
                        end = 10.dp
                    ),
                    text = lift.liftName,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (!isEdit) {
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
            }
            if (!isEdit) {
                val localDensity = LocalDensity.current
                var noteTextFieldHeight by remember { mutableStateOf(40.dp) }
                var note by remember(lift.note) { mutableStateOf(lift.note ?: "") }
                val focusManager: FocusManager = LocalFocusManager.current
                val onDone: (clearFocus: Boolean) -> Unit = { clearFocus ->
                    if (clearFocus) {
                        focusManager.clearFocus()
                    }
                    onNoteChanged(lift.id, note)
                }
                LiftLabOutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(noteTextFieldHeight)
                        .padding(start = 15.dp, end = 10.dp),
                    focusManager = focusManager,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onDone(true)
                        this.defaultKeyboardAction(ImeAction.Done)
                    }),
                    onDone = onDone, // Invoked via BackHandler since user will not trigger keyboard action above
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        top = 7.dp,
                        bottom = 7.dp,
                        end = 2.dp
                    ),
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
                    value = note,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.lift_note),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    },
                    onValueChange = {
                        note = it
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
                        onDeleteMyoRepSet(
                            lift.id,
                            set.position,
                            (set as LoggingMyoRepSetDto).myoRepSetPosition!!
                        )
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
                                onWeightChanged(
                                    lift.id,
                                    set.position,
                                    (set as? LoggingMyoRepSetDto)?.myoRepSetPosition,
                                    it
                                )
                            },
                            onRepsChanged = {
                                onRepsChanged(
                                    lift.id,
                                    set.position,
                                    (set as? LoggingMyoRepSetDto)?.myoRepSetPosition,
                                    it
                                )
                            },
                            toggleRpePicker = {
                                if (it) {
                                    onShowRpePicker(lift.id, set.position, (set as? LoggingMyoRepSetDto)?.myoRepSetPosition)
                                } else {
                                    onHideRpePicker()
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
                                onHideRpePicker()
                            },
                            onUndoCompletion = {
                                onUndoSetCompletion(
                                    lift.position,
                                    set.position,
                                    (set as? LoggingMyoRepSetDto)?.myoRepSetPosition,
                                )
                            },
                            onAddSpacer = {
                                onUpdatePickerSpacer(it)
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}