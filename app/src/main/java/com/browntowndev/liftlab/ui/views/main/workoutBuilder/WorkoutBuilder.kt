package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.Utils.General.Companion.percentageStringToFloat
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.ui.composables.ConfirmationDialog
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.PercentagePicker
import com.browntowndev.liftlab.ui.composables.ReorderableLazyColumn
import com.browntowndev.liftlab.ui.composables.RpeKeyboard
import com.browntowndev.liftlab.ui.composables.TextFieldDialog
import com.browntowndev.liftlab.ui.composables.VolumeChipBottomSheet
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet.CustomSettings
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns.ProgressionSchemeDropdown
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns.WavePatternDropdown
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutBuilder(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    screenId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToLiftLibrary: (route: String) -> Unit,
    workoutId: Long,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit,
) {
    val workoutBuilderViewModel: WorkoutBuilderViewModel = koinViewModel {
        parametersOf(
            workoutId,
            onNavigateBack,
            SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
        )
    }
    val state by workoutBuilderViewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.workout) {
        if (state.workout != null) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    controlName = Screen.TITLE,
                    payload = state.workout!!.name
                )
            )
        }
    }

    workoutBuilderViewModel.registerEventBus()
    EventBusDisposalEffect(screenId = screenId, viewModelToUnregister = workoutBuilderViewModel)

    if(!state.isReordering) {
        VolumeChipBottomSheet(
            placeAboveBottomNavBar = false,
            title = "Workout Volume",
            combinedVolumeChipLabels = state.combinedVolumeTypes,
            primaryVolumeChipLabels = state.primaryVolumeTypes,
            secondaryVolumeChipLabels = state.secondaryVolumeTypes,
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                var scrollSpacerSize by remember { mutableStateOf(0.dp) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    state = listState,
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(state.workout?.lifts ?: listOf(), { it.id }) { workoutLift ->
                        val standardLift = workoutLift as? StandardWorkoutLiftDto
                        val customLift = workoutLift as? CustomWorkoutLiftDto
                        val incrementOverride = remember(
                            key1 = workoutLift.incrementOverride,
                        ) {
                            workoutLift.incrementOverride ?: SettingsManager.getSetting(
                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
                            )
                        }
                        val restTime = remember(
                            key1 = workoutLift.restTime,
                        ) {
                            workoutLift.restTime ?: SettingsManager.getSetting(
                                SettingsManager.SettingNames.REST_TIME,
                                SettingsManager.SettingNames.DEFAULT_REST_TIME
                            ).toDuration(DurationUnit.MILLISECONDS)
                        }
                        val customLiftsVisible = remember(customLift) { customLift != null }
                        val showCustomSetsOption = remember(workoutLift.progressionScheme) {
                            workoutLift.progressionScheme != ProgressionScheme.LINEAR_PROGRESSION &&
                                    workoutLift.progressionScheme != ProgressionScheme.WAVE_LOADING_PROGRESSION
                        }
                        val showDeloadWeekOption = remember(workoutLift.progressionScheme) {
                            workoutLift.progressionScheme != ProgressionScheme.LINEAR_PROGRESSION &&
                                    SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
                        }

                        LiftCard(
                            liftName = workoutLift.liftName,
                            increment = incrementOverride,
                            restTime = restTime,
                            restTimerEnabled = workoutLift.restTimerEnabled,
                            movementPattern = workoutLift.liftMovementPattern,
                            hasCustomLiftSets = customLiftsVisible,
                            showCustomSetsOption = showCustomSetsOption,
                            currentDeloadWeek = workoutLift.deloadWeek,
                            showDeloadWeekOption = showDeloadWeekOption,
                            onCustomLiftSetsToggled = {
                                if (!it) {
                                    workoutBuilderViewModel.toggleHasCustomLiftSets(
                                        workoutLiftId = workoutLift.id,
                                        enableCustomSets = false,
                                    )
                                } else {
                                    workoutBuilderViewModel.toggleHasCustomLiftSets(
                                        workoutLiftId = workoutLift.id,
                                        enableCustomSets = true,
                                    )
                                }
                            },
                            onReplaceLift = {
                                val liftLibraryRoute = LiftLibraryScreen.navigation.route
                                    .replace("{callerRoute}", WorkoutBuilderScreen.navigation.route)
                                    .replace("{workoutId}", state.workout!!.id.toString())
                                    .replace("{workoutLiftId}", workoutLift.id.toString())
                                    .replace(
                                        "{movementPattern}",
                                        workoutLift.liftMovementPattern.displayName()
                                    )

                                onNavigateToLiftLibrary(liftLibraryRoute)
                            },
                            onDeleteLift = {
                                workoutBuilderViewModel.toggleMovementPatternDeletionModal(
                                    workoutLift.id
                                )
                            },
                            onChangeDeloadWeek = {
                                workoutBuilderViewModel.updateDeloadWeek(workoutLift.id, it)
                            },
                            onChangeRestTime = { newRestTime, enabled ->
                                workoutBuilderViewModel.setRestTime(
                                    workoutLiftId = workoutLift.id,
                                    newRestTime = newRestTime,
                                    enabled = enabled,
                                )
                            },
                            onChangeIncrement = { newIncrement ->
                                workoutBuilderViewModel.setIncrementOverride(
                                    workoutLiftId = workoutLift.id,
                                    newIncrement = newIncrement,
                                )
                            },
                        ) {
                            ProgressionSchemeDropdown(
                                modifier = Modifier.padding(start = 20.dp),
                                text = workoutLift.progressionScheme.displayName(),
                                hasCustomSets = customLiftsVisible,
                                onChangeProgressionScheme = {
                                    workoutBuilderViewModel.setLiftProgressionScheme(
                                        workoutLift.id,
                                        it
                                    )
                                },
                            )
                            WavePatternDropdown(
                                workoutLiftId = workoutLift.id,
                                stepSize = (workoutLift as? StandardWorkoutLiftDto)?.stepSize,
                                progressionScheme = workoutLift.progressionScheme,
                                workoutLiftStepSizeOptions = state.workoutLiftStepSizeOptions,
                                onUpdateStepSize = { workoutLiftId, newStepSize ->
                                    workoutBuilderViewModel.updateStepSize(workoutLiftId = workoutLiftId, newStepSize = newStepSize)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // These keep the TextField from flashing between the default & actual value when
                            // custom settings are toggled
                            val repRangeBottom by remember(
                                key1 = standardLift?.repRangeBottom,
                                key2 = customLift?.customLiftSets?.firstOrNull()?.repRangeBottom
                            ) {
                                mutableIntStateOf(
                                    standardLift?.repRangeBottom
                                        ?: customLift!!.customLiftSets.first().repRangeBottom
                                )
                            }
                            val repRangeTop by remember(
                                key1 = standardLift?.repRangeTop,
                                key2 = customLift?.customLiftSets?.firstOrNull()?.repRangeTop
                            ) {
                                mutableIntStateOf(
                                    standardLift?.repRangeTop
                                        ?: customLift!!.customLiftSets.first().repRangeTop
                                )
                            }
                            val rpeTarget by remember(
                                key1 = standardLift?.rpeTarget,
                                key2 = customLift?.customLiftSets?.firstOrNull()?.rpeTarget
                            ) {
                                mutableFloatStateOf(
                                    standardLift?.rpeTarget
                                        ?: customLift!!.customLiftSets.first().rpeTarget
                                )
                            }

                            Row {
                                Spacer(modifier = Modifier.width(10.dp))
                                StandardSettings(
                                    isVisible = !customLiftsVisible,
                                    listState = listState,
                                    repRangeBottom = repRangeBottom,
                                    repRangeTop = repRangeTop,
                                    progressionScheme = workoutLift.progressionScheme,
                                    setCount = workoutLift.setCount,
                                    rpeTarget = rpeTarget,
                                    onSetCountChanged = {
                                        workoutBuilderViewModel.setLiftSetCount(
                                            workoutLiftId = workoutLift.id,
                                            newSetCount = it,
                                        )
                                    },
                                    onRepRangeBottomChanged = {
                                        workoutBuilderViewModel.setLiftRepRangeBottom(
                                            workoutLiftId = workoutLift.id,
                                            newRepRangeBottom = it
                                        )
                                    },
                                    onRepRangeTopChanged = {
                                        workoutBuilderViewModel.setLiftRepRangeTop(
                                            workoutLiftId = workoutLift.id,
                                            newRepRangeTop = it
                                        )
                                    },
                                    onConfirmRepRangeBottom = {
                                        workoutBuilderViewModel.confirmStandardSetRepRangeBottom(
                                            workoutLiftId = workoutLift.id,
                                        )
                                    },
                                    onConfirmRepRangeTop = {
                                        workoutBuilderViewModel.confirmStandardSetRepRangeTop(
                                            workoutLiftId = workoutLift.id,
                                        )
                                    },
                                    onRpeTargetChanged = {
                                        workoutBuilderViewModel.setLiftRpeTarget(
                                            workoutLiftId = workoutLift.id,
                                            newRpeTarget = it
                                                ?: 8f // should never be null, but just in case
                                        )
                                    },
                                    onToggleRpePicker = {
                                        workoutBuilderViewModel.togglePicker(
                                            workoutLiftId = workoutLift.id,
                                            visible = it,
                                            currentRpe = rpeTarget,
                                            type = PickerType.Rpe,
                                        )
                                    },
                                    onPixelOverflowChanged = {
                                        scrollSpacerSize = it
                                    }
                                )
                            }
                            Row {
                                // Save this here and then update it after the custom lifts are rendered
                                // so the lifts don't disappear until after animation}
                                var nonDisappearCustomLifts by remember {
                                    mutableStateOf(
                                        customLift?.customLiftSets
                                            ?: listOf()
                                    )
                                }
                                val customLifts = remember(customLift?.customLiftSets) {
                                    customLift?.customLiftSets
                                        ?: nonDisappearCustomLifts
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                CustomSettings(
                                    isVisible = customLiftsVisible,
                                    detailExpansionStates = state.detailExpansionStates[workoutLift.id]
                                        ?: hashSetOf(),
                                    listState = listState,
                                    customSets = customLifts,
                                    onAddSet = { workoutBuilderViewModel.addSet(workoutLiftId = workoutLift.id) },
                                    onDeleteSet = {
                                        workoutBuilderViewModel.deleteSet(
                                            workoutLiftId = workoutLift.id,
                                            it
                                        )
                                    },
                                    onRepRangeBottomChanged = { position, newRepRangeBottom ->
                                        workoutBuilderViewModel.setCustomSetRepRangeBottom(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            newRepRangeBottom = newRepRangeBottom,
                                        )
                                    },
                                    onRepRangeTopChanged = { position, newRepRangeTop ->
                                        workoutBuilderViewModel.setCustomSetRepRangeTop(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            newRepRangeTop = newRepRangeTop
                                        )
                                    },
                                    onConfirmRepRangeBottom = { position ->
                                        workoutBuilderViewModel.confirmCustomSetRepRangeBottom(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                        )
                                    },
                                    onConfirmRepRangeTop = { position ->
                                        workoutBuilderViewModel.confirmCustomSetRepRangeTop(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                        )
                                    },
                                    onRepFloorChanged = { position, newRepFloor ->
                                        workoutBuilderViewModel.setCustomSetRepFloor(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            newRepFloor = newRepFloor,
                                        )
                                    },
                                    onSetMatchingChanged = { position, enabled ->
                                        workoutBuilderViewModel.setCustomSetUseSetMatching(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            setMatching = enabled,
                                        )
                                    },
                                    onMatchSetGoalChanged = { position, newMatchSetGoal ->
                                        workoutBuilderViewModel.setCustomSetMatchSetGoal(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            newMatchSetGoal = newMatchSetGoal,
                                        )
                                    },
                                    onCustomSetTypeChanged = { position, newSetType ->
                                        workoutBuilderViewModel.changeCustomSetType(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            newSetType = newSetType,
                                        )
                                    },
                                    onMaxSetsChanged = { position, newMaxSets ->
                                        workoutBuilderViewModel.setCustomSetMaxSets(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            newMaxSets = newMaxSets,
                                        )
                                    },
                                    toggleRpePicker = { position, visible, currentRpe ->
                                        workoutBuilderViewModel.togglePicker(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            visible = visible,
                                            currentRpe = currentRpe,
                                            type = PickerType.Rpe,
                                        )
                                    },
                                    togglePercentagePicker = { position, visible ->
                                        workoutBuilderViewModel.togglePicker(
                                            workoutLiftId = workoutLift.id,
                                            position = position,
                                            visible = visible,
                                            type = PickerType.Percentage,
                                        )
                                    },
                                    toggleDetailsExpansion = {
                                        workoutBuilderViewModel.toggleDetailExpansion(
                                            workoutLiftId = workoutLift.id,
                                            position = it
                                        )
                                    },
                                    onPixelOverflowChanged = {
                                        scrollSpacerSize = it
                                    }
                                )
                                LaunchedEffect(customLift?.customLiftSets) {
                                    if (customLift?.customLiftSets == null) {
                                        delay(600)
                                    }
                                    nonDisappearCustomLifts = customLift?.customLiftSets ?: listOf()
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = {
                                    val liftLibraryRoute = LiftLibraryScreen.navigation.route
                                        .replace("{workoutId}", state.workout!!.id.toString())
                                        .replace(
                                            "{addAtPosition}",
                                            state.workout!!.lifts.count().toString()
                                        )
                                    onNavigateToLiftLibrary(liftLibraryRoute)
                                }
                            ) {
                                Text(
                                    text = "Add Movement Pattern",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 17.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                        Spacer(modifier = modifier.height(scrollSpacerSize))

                        LaunchedEffect(scrollSpacerSize) {
                            if (scrollSpacerSize > 0.dp) {
                                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                            }
                        }
                    }
                }

                RpeKeyboard(
                    visible = state.pickerState?.type == PickerType.Rpe,
                    selectedRpe = state.pickerState?.currentRpe,
                    onRpeSelected = {
                        if (state.pickerState!!.setPosition == null) {
                            workoutBuilderViewModel.setLiftRpeTarget(
                                workoutLiftId = state.pickerState!!.workoutLiftId!!,
                                it,
                            )
                        } else {
                            workoutBuilderViewModel.setCustomSetRpeTarget(
                                workoutLiftId = state.pickerState!!.workoutLiftId!!,
                                position = state.pickerState!!.setPosition!!,
                                newRpeTarget = it,
                            )
                        }
                    },
                    onClosed = {
                        scrollSpacerSize = 0.dp
                    }
                )
                PercentagePicker(
                    visible = state.pickerState?.type == PickerType.Percentage,
                    onPercentageSelected = {
                        workoutBuilderViewModel.setCustomSetDropPercentage(
                            workoutLiftId = state.pickerState!!.workoutLiftId!!,
                            position = state.pickerState!!.setPosition!!,
                            newDropPercentage = percentageStringToFloat(it),
                        )
                    }
                )
            }
        }
    } else {
        ReorderableLazyColumn(
            paddingValues = paddingValues,
            items = remember(state.workout!!.lifts) { state.workout!!.lifts.fastMap { ReorderableListItem(it.liftName, it.id) } },
            saveReorder = { workoutBuilderViewModel.reorderLifts(it) },
            cancelReorder = { workoutBuilderViewModel.toggleReorderLifts() }
        )
    }

    if (state.isEditingName) {
        TextFieldDialog(
            header = "Rename ${state.workout!!.name}",
            initialTextFieldValue = state.workout!!.name,
            onConfirm = { workoutBuilderViewModel.updateWorkoutName(it) },
            onCancel = { workoutBuilderViewModel.toggleWorkoutRenameModal() },
        )
    }

    if (state.workoutLiftIdToDelete != null) {
        ConfirmationDialog(
            header = "Delete ${state.movementPatternOfDeletingWorkoutLift}?",
            textAboveContent = "Are you sure you want to delete this movement pattern?",
            onConfirm = {
                workoutBuilderViewModel.deleteMovementPattern()
            },
            onCancel = {
                workoutBuilderViewModel.toggleMovementPatternDeletionModal()
            }
        )
    }
}
