package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.convertToDouble
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet.CustomSettings
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns.LiftDropdown
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns.ProgressionSchemeDropdown
import com.browntowndev.liftlab.ui.views.utils.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.utils.PercentagePicker
import com.browntowndev.liftlab.ui.views.utils.RpePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutBuilder(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    navHostController: NavHostController,
    workoutId: Long,
    workoutBuilderViewModel: WorkoutBuilderViewModel = koinViewModel { parametersOf(workoutId, navHostController) },
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<String?>) -> Unit
) {
    val state by workoutBuilderViewModel.state.collectAsState()
    val listState = rememberLazyListState()
    workoutBuilderViewModel.registerEventBus()

    LaunchedEffect(state.workout) {
        mutateTopAppBarControlValue(
            AppBarMutateControlRequest(
                controlName = Screen.SUBTITLE,
                payload = state.workout?.name ?: ""
            )
        )
    }

    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = workoutBuilderViewModel)

    Box(contentAlignment = Alignment.BottomCenter){
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
                var customLiftsVisible by remember { mutableStateOf(customLift != null) }

                LiftCard(
                    liftName = workoutLift.liftName,
                    category = workoutLift.liftMovementPattern,
                    hasCustomLiftSets = workoutLift !is StandardWorkoutLiftDto,
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
                    onReplaceMovementPattern = { /*TODO*/ },
                    onReplaceLift = { /*TODO*/ },
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(10.dp))
                        ProgressionSchemeDropdown(
                            text = workoutLift.progressionScheme.displayName(),
                            onChangeProgressionScheme = {
                                workoutBuilderViewModel.setLiftProgressionScheme(workoutLift.id, it)
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // These keep the TextField from flashing between the default & actual value when
                    // custom settings are toggled
                    var repRangeBottom by remember { mutableStateOf(standardLift?.repRangeBottom ?: 8) }
                    var repRangeTop by remember { mutableStateOf(standardLift?.repRangeTop ?: 10) }
                    var rpeTarget by remember { mutableStateOf(standardLift?.rpeTarget ?: 8.toDouble()) }

                    LaunchedEffect(
                        key1 = standardLift?.repRangeBottom,
                        key2 = standardLift?.repRangeTop,
                        key3 = standardLift?.rpeTarget
                    ) {
                        repRangeBottom = if (standardLift?.repRangeBottom != null) standardLift.repRangeBottom else repRangeBottom
                        repRangeTop = if (standardLift?.repRangeTop != null) standardLift.repRangeTop else repRangeTop
                        rpeTarget = if (standardLift?.rpeTarget != null) standardLift.rpeTarget else rpeTarget
                    }

                    for(set in customLift?.customLiftSets ?: listOf()) {
                        when (set) {
                            is StandardSetDto ->
                                LaunchedEffect(set.rpeTarget) {
                                    rpeTarget = set.rpeTarget
                                }
                            is DropSetDto ->
                                LaunchedEffect(key1 = set.rpeTarget) {
                                    rpeTarget = set.rpeTarget
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
                                workoutBuilderViewModel.setUseSetMatching(
                                    workoutLiftId = workoutLift.id,
                                    position = position,
                                    setMatching = enabled,
                                )
                            },
                            onMatchSetGoalChanged = { position, newMatchSetGoal ->
                                workoutBuilderViewModel.setMatchSetGoal(
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
                        modifier = Modifier.clickable { /*TODO add movement pattern*/ },
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
                    newDropPercentage = convertToDouble(it),
                )
            }
        )
    }
}
