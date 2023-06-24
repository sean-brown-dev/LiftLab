package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.convertToFloat
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LabScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet.CustomSettings
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns.ProgressionSchemeDropdown
import com.browntowndev.liftlab.ui.views.utils.ConfirmationModal
import com.browntowndev.liftlab.ui.views.utils.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.utils.PercentagePicker
import com.browntowndev.liftlab.ui.views.utils.ReorderableLazyColumn
import com.browntowndev.liftlab.ui.views.utils.RpePicker
import com.browntowndev.liftlab.ui.views.utils.TextFieldModal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutBuilder(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    navHostController: NavHostController,
    workoutId: Long,
    workoutBuilderViewModel: WorkoutBuilderViewModel = koinViewModel {
        parametersOf(
            workoutId,
            navHostController
        )
    },
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit
) {
    val state by workoutBuilderViewModel.state.collectAsState()
    val listState = rememberLazyListState()

    BackHandler(true) {
        navHostController.popBackStack()
        navHostController.navigate(LabScreen.navigation.route)
    }

    LaunchedEffect(state.workout) {
        mutateTopAppBarControlValue(
            AppBarMutateControlRequest(
                controlName = Screen.SUBTITLE,
                payload = state.workout?.name ?: ""
            )
        )
    }

    workoutBuilderViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = workoutBuilderViewModel)

    if(!state.isReordering) {
        Box(contentAlignment = Alignment.BottomCenter) {
            var scrollSpacerSize by remember { mutableStateOf(0.dp) }
            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
            ) {
                item {
                    Spacer(modifier.height(15.dp))
                }
                items(state.workout?.lifts ?: listOf(), { it.id }) { workoutLift ->
                    val standardLift = workoutLift as? StandardWorkoutLiftDto
                    val customLift = workoutLift as? CustomWorkoutLiftDto
                    var overrideAppliedAtLiftLevel by remember {
                        mutableStateOf(workoutLift.incrementOverride == null && workoutLift.liftIncrementOverride != null)
                    }
                    var restTimeAppliedAtLiftLevel by remember {
                        mutableStateOf(workoutLift.restTime == null && workoutLift.liftRestTime != null)
                    }
                    var incrementOverride by remember {
                        mutableStateOf(
                            workoutLift.incrementOverride ?: workoutLift.liftIncrementOverride
                            ?: SettingsManager.getSetting(
                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
                            )
                        )
                    }
                    var restTime by remember {
                        mutableStateOf(
                            workoutLift.restTime ?: workoutLift.liftRestTime
                            ?: SettingsManager.getSetting(
                                SettingsManager.SettingNames.REST_TIME,
                                SettingsManager.SettingNames.DEFAULT_REST_TIME
                            ).toDuration(DurationUnit.MILLISECONDS)
                        )
                    }
                    var customLiftsVisible by remember { mutableStateOf(customLift != null) }
                    var showCustomSetsOption by remember {
                        mutableStateOf(workoutLift.progressionScheme != ProgressionScheme.LINEAR_PROGRESSION &&
                                workoutLift.progressionScheme != ProgressionScheme.WAVE_LOADING_PROGRESSION)
                    }
                    var deloadWeek by remember { mutableStateOf(workoutLift.deloadWeek ?: state.programDeloadWeek) }
                    var showDeloadWeekOption by remember { mutableStateOf(workoutLift.progressionScheme != ProgressionScheme.LINEAR_PROGRESSION) }

                    LaunchedEffect(workoutLift.progressionScheme) {
                        showCustomSetsOption = workoutLift.progressionScheme != ProgressionScheme.LINEAR_PROGRESSION &&
                                workoutLift.progressionScheme != ProgressionScheme.WAVE_LOADING_PROGRESSION

                        showDeloadWeekOption = workoutLift.progressionScheme != ProgressionScheme.LINEAR_PROGRESSION
                    }

                    LaunchedEffect(workoutLift.deloadWeek) {
                        deloadWeek = workoutLift.deloadWeek ?: state.programDeloadWeek
                    }

                    LaunchedEffect(
                        key1 = workoutLift.incrementOverride,
                        key2 = workoutLift.liftIncrementOverride,
                    ) {
                        overrideAppliedAtLiftLevel =
                            workoutLift.incrementOverride == null && workoutLift.liftIncrementOverride != null

                        incrementOverride =
                            workoutLift.incrementOverride ?: workoutLift.liftIncrementOverride
                                    ?: SettingsManager.getSetting(
                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
                            )
                    }

                    LaunchedEffect(
                        key1 = workoutLift.restTime,
                        key2 = workoutLift.liftRestTime,
                    ) {

                        restTimeAppliedAtLiftLevel =
                            workoutLift.restTime == null && workoutLift.liftRestTime != null

                        restTime = workoutLift.restTime ?: workoutLift.liftRestTime
                                ?: SettingsManager.getSetting(
                            SettingsManager.SettingNames.REST_TIME,
                            SettingsManager.SettingNames.DEFAULT_REST_TIME
                        ).toDuration(DurationUnit.MILLISECONDS)
                    }

                    LiftCard(
                        liftName = workoutLift.liftName,
                        increment = incrementOverride,
                        restTime = restTime,
                        restTimeAppliedAcrossWorkouts = restTimeAppliedAtLiftLevel,
                        incrementAppliedAcrossWorkouts = overrideAppliedAtLiftLevel,
                        movementPattern = workoutLift.liftMovementPattern,
                        hasCustomLiftSets = customLiftsVisible,
                        showCustomSetsOption = showCustomSetsOption,
                        currentDeloadWeek = deloadWeek,
                        showDeloadWeekOption = showDeloadWeekOption,
                        onCustomLiftSetsToggled = {
                            if (!it) {
                                customLiftsVisible = false
                                // Allow them to collapse before removing them
                                this.launch {
                                    delay(510)
                                    workoutBuilderViewModel.toggleHasCustomLiftSets(
                                        workoutLiftId = workoutLift.id,
                                        enableCustomSets = false,
                                    )
                                }
                            } else {
                                workoutBuilderViewModel.toggleHasCustomLiftSets(
                                    workoutLiftId = workoutLift.id,
                                    enableCustomSets = true,
                                )
                                customLiftsVisible = true
                            }
                        },
                        onReplaceLift = {
                            navHostController.navigate(
                                LiftLibraryScreen.navigation.route +
                                        "?workoutId=${state.workout!!.id}" +
                                        "&workoutLiftId=${workoutLift.id}" +
                                        "&movementPattern=${workoutLift.liftMovementPattern.displayName()}")
                        },
                        onDeleteLift = {
                            workoutBuilderViewModel.toggleMovementPatternDeletionModal(workoutLift.id)
                        },
                        onChangeDeloadWeek = {
                            workoutBuilderViewModel.toggleDeloadWeekModal(workoutLift)
                        },
                        onChangeRestTime = { newRestTime, applyAcrossWorkouts ->
                            workoutBuilderViewModel.setRestTime(
                                workoutLiftId = workoutLift.id,
                                newRestTime = newRestTime,
                                applyAcrossWorkouts = applyAcrossWorkouts,
                            )
                        },
                        onChangeIncrement = { },
                        onChangeRestTimeAppliedAcrossWorkouts = {},
                        onChangeIncrementAppliedAcrossWorkouts = {},
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(10.dp))
                            ProgressionSchemeDropdown(
                                text = workoutLift.progressionScheme.displayName(),
                                hasCustomSets = customLiftsVisible,
                                onChangeProgressionScheme = {
                                    workoutBuilderViewModel.setLiftProgressionScheme(workoutLift.id, it)
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // These keep the TextField from flashing between the default & actual value when
                        // custom settings are toggled
                        var repRangeBottom by remember { mutableStateOf(standardLift?.repRangeBottom ?: customLift!!.customLiftSets.first().repRangeBottom) }
                        var repRangeTop by remember { mutableStateOf(standardLift?.repRangeTop ?: customLift!!.customLiftSets.first().repRangeTop) }
                        var rpeTarget by remember { mutableStateOf(standardLift?.rpeTarget ?: customLift!!.customLiftSets.first().rpeTarget) }

                        LaunchedEffect(
                            key1 = standardLift?.repRangeBottom,
                            key2 = standardLift?.repRangeTop,
                            key3 = standardLift?.rpeTarget,
                        ) {
                            repRangeBottom = if (standardLift?.repRangeBottom != null) standardLift.repRangeBottom else repRangeBottom
                            repRangeTop = if (standardLift?.repRangeTop != null) standardLift.repRangeTop else repRangeTop
                            rpeTarget = if (standardLift?.rpeTarget != null) standardLift.rpeTarget else rpeTarget
                        }

                        LaunchedEffect(
                            key1 = customLift?.customLiftSets?.first()?.repRangeBottom,
                            key2 = customLift?.customLiftSets?.first()?.repRangeTop,
                            key3 = customLift?.customLiftSets?.first()?.rpeTarget,
                        ) {
                            val changedTopSet = customLift?.customLiftSets?.first()
                            repRangeBottom = changedTopSet?.repRangeBottom ?: repRangeBottom
                            repRangeTop = changedTopSet?.repRangeTop ?: repRangeTop
                            rpeTarget = changedTopSet?.rpeTarget ?: rpeTarget
                        }

                        (customLift?.customLiftSets ?: listOf()).fastForEach { set ->
                            when (set) {
                                is StandardSetDto ->
                                    LaunchedEffect(key1 = set.rpeTarget, key2 = set.repRangeBottom, key3 = set.repRangeTop) {
                                        repRangeTop = set.repRangeTop
                                        repRangeBottom = set.repRangeBottom
                                        rpeTarget = set.rpeTarget
                                    }
                                is DropSetDto ->
                                    LaunchedEffect(key1 = set.rpeTarget, key2 = set.repRangeBottom, key3 = set.repRangeTop) {
                                        repRangeTop = set.repRangeTop
                                        repRangeBottom = set.repRangeBottom
                                        rpeTarget = set.rpeTarget
                                    }
                                is MyoRepSetDto ->
                                    LaunchedEffect(key1 = set.repRangeBottom, key2 = set.repRangeTop) {
                                        repRangeTop = set.repRangeTop
                                        repRangeBottom = set.repRangeBottom
                                    }
                                else -> {}
                            }
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
                                onRpeTargetChanged = {
                                    workoutBuilderViewModel.setLiftRpeTarget(
                                        workoutLiftId = workoutLift.id,
                                        newRpeTarget = it
                                    )
                                },
                                onToggleRpePicker = {
                                    workoutBuilderViewModel.togglePicker(
                                        workoutLiftId = workoutLift.id,
                                        visible = it,
                                        type = PickerType.Rpe,
                                    )
                                },
                                onPixelOverflowChanged = {
                                    scrollSpacerSize = it
                                }
                            )
                        }
                        Row {
                            Spacer(modifier = Modifier.width(10.dp))
                            CustomSettings(
                                isVisible = customLiftsVisible,
                                detailExpansionStates = state.detailExpansionStates[workoutLift.id] ?: hashSetOf(),
                                listState = listState,
                                customSets = customLift?.customLiftSets ?: listOf(),
                                onAddSet = { workoutBuilderViewModel.addSet(workoutLiftId = workoutLift.id) },
                                onDeleteSet = { workoutBuilderViewModel.deleteSet(workoutLiftId = workoutLift.id, it) },
                                onRepRangeBottomChanged = { position, newRepRangeBottom ->
                                    if (position == 0) repRangeBottom = newRepRangeBottom
                                    workoutBuilderViewModel.setCustomSetRepRangeBottom(
                                        workoutLiftId = workoutLift.id,
                                        position = position,
                                        newRepRangeBottom = newRepRangeBottom,
                                    )
                                },
                                onRepRangeTopChanged = { position, newRepRangeTop ->
                                    if (position == 0) repRangeTop = newRepRangeTop
                                    workoutBuilderViewModel.setCustomSetRepRangeTop(
                                        workoutLiftId = workoutLift.id,
                                        position = position,
                                        newRepRangeTop = newRepRangeTop
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
                                toggleRpePicker = { position, visible ->
                                    workoutBuilderViewModel.togglePicker(
                                        workoutLiftId = workoutLift.id,
                                        position = position,
                                        visible = visible,
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
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier.clickable {
                                navHostController.navigate(
                                    LiftLibraryScreen.navigation.route +
                                            "?workoutId=${state.workout!!.id}" +
                                            "&addAtPosition=${state.workout!!.lifts.count()}")
                            },
                            text = "Add Movement Pattern",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(25.dp))
                    Spacer(modifier = modifier.height(scrollSpacerSize))

                    LaunchedEffect(scrollSpacerSize) {
                        if (scrollSpacerSize > 0.dp) {
                            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                        }
                    }
                }
            }

            RpePicker(
                visible = state.pickerState?.type == PickerType.Rpe,
                onRpeSelected = {
                    if (state.pickerState!!.position == null) {
                        workoutBuilderViewModel.setLiftRpeTarget(
                            workoutLiftId = state.pickerState!!.workoutLiftId,
                            it,
                        )
                    } else {
                        workoutBuilderViewModel.setCustomSetRpeTarget(
                            workoutLiftId = state.pickerState!!.workoutLiftId,
                            position = state.pickerState!!.position!!,
                            newRpeTarget = it,
                        )
                    }
                }
            )
            PercentagePicker(
                visible = state.pickerState?.type == PickerType.Percentage,
                onPercentageSelected = {
                    workoutBuilderViewModel.setCustomSetDropPercentage(
                        workoutLiftId = state.pickerState!!.workoutLiftId,
                        position = state.pickerState!!.position!!,
                        newDropPercentage = convertToFloat(it),
                    )
                }
            )
        }
    } else {
        ReorderableLazyColumn(
            paddingValues = paddingValues,
            items = state.workout!!.lifts.fastMap { ReorderableListItem(it.liftName, it.id) },
            saveReorder = { workoutBuilderViewModel.reorderLifts(it) },
            cancelReorder = { workoutBuilderViewModel.toggleReorderLifts() }
        )
    }

    if (state.isEditingName) {
        TextFieldModal(
            header = "Rename ${state.workout!!.name}",
            initialTextFieldValue = state.workout!!.name,
            onConfirm = { workoutBuilderViewModel.updateWorkoutName(it) },
            onCancel = { workoutBuilderViewModel.toggleWorkoutRenameModal() },
        )
    }

    if (state.workoutLiftToChangeDeloadWeek != null) {
        TextFieldModal(
            header = "Change Deload Week",
            initialTextFieldValue = state.workoutLiftToChangeDeloadWeek!!.deloadWeek ?: state.programDeloadWeek!!,
            onConfirm = { workoutBuilderViewModel.updateDeloadWeek(it) },
            onCancel = { workoutBuilderViewModel.toggleDeloadWeekModal() },
        )
    }

    if (state.workoutLiftIdToDelete != null) {
        ConfirmationModal(
            header = "Delete ${state.movementPatternOfDeletingWorkoutLift}?",
            body = "Are you sure you want to delete this movement pattern?",
            onConfirm = {
                workoutBuilderViewModel.deleteMovementPattern()
            },
            onCancel = {
                workoutBuilderViewModel.toggleMovementPatternDeletionModal()
            }
        )
    }
}
