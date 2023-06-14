package com.browntowndev.liftlab.ui.views.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.displayNameShort
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.views.utils.DoubleTextField
import com.browntowndev.liftlab.ui.views.utils.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.utils.IconDropdown
import com.browntowndev.liftlab.ui.views.utils.IntegerOnlyTextField
import com.browntowndev.liftlab.ui.views.utils.LabeledCheckBox
import com.browntowndev.liftlab.ui.views.utils.RpePicker
import com.browntowndev.liftlab.ui.views.utils.TextDropdown
import com.browntowndev.liftlab.ui.views.utils.TextDropdownTextAnchor
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
        LazyColumn(
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

                    Row {
                        Spacer(modifier = Modifier.width(10.dp))
                        StandardSettings(
                            isVisible = !customLiftsVisible,
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
                                workoutBuilderViewModel.toggleRpePicker(
                                    workoutLiftId = workoutLift.id,
                                    visible = it,
                                )
                            }
                        )
                    }
                    Row {
                        Spacer(modifier = Modifier.width(10.dp))
                        CustomSettings(
                            isVisible = customLiftsVisible,
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
                            onRpeTargetChanged = { position, newRpeTarget ->
                                if (position == 0) rpeTarget = newRpeTarget
                                workoutBuilderViewModel.setCustomSetRpeTarget(
                                    workoutLiftId = workoutLift.id,
                                    position = position,
                                    newRpeTarget = newRpeTarget
                                )
                            },
                            onDropPercentageChanged = { position, newDropPercentage ->
                                workoutBuilderViewModel.setCustomSetDropPercentage(
                                    workoutLiftId = workoutLift.id,
                                    position = position,
                                    newDropPercentage = newDropPercentage,
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
                                    newSetType = newSetType
                                )
                            },
                            toggleRpePicker = { position, visible ->
                                workoutBuilderViewModel.toggleRpePicker(
                                    workoutLiftId = workoutLift.id,
                                    position = position,
                                    visible = visible,
                                )
                            },
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
            }
        }

        RpePicker(
            visible = state.rpePickerState != null,
            onRpeSelected = {
                if (state.rpePickerState!!.position == null) {
                    workoutBuilderViewModel.setLiftRpeTarget(
                        workoutLiftId = state.rpePickerState!!.workoutLiftId,
                        it,
                    )
                } else {
                    workoutBuilderViewModel.setCustomSetRpeTarget(
                        workoutLiftId = state.rpePickerState!!.workoutLiftId,
                        position = state.rpePickerState!!.position!!,
                        newRpeTarget = it,
                    )
                }
            }
        )
    }
}

@Composable
fun LiftCard(
    liftName: String,
    category: MovementPattern,
    hasCustomLiftSets: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceMovementPattern: () -> Unit,
    onReplaceLift: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Column (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 15.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp, pressedElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(8.dp),
        ) {
            LiftCardHeader(
                category = category,
                liftName = liftName,
                hasCustomLiftSets = hasCustomLiftSets,
                onCustomLiftSetsToggled = onCustomLiftSetsToggled,
                onReplaceMovementPattern = onReplaceMovementPattern,
                onReplaceLift = onReplaceLift,
            )
            Spacer(modifier = Modifier.height(15.dp))
            content()
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun LiftCardHeader(
    category: MovementPattern,
    liftName: String,
    hasCustomLiftSets: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceMovementPattern: () -> Unit,
    onReplaceLift: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.padding(15.dp, 5.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.displayName(),
                fontSize = 25.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = liftName,
                overflow = TextOverflow.Ellipsis,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        LiftDropdown(
            hasCustomLiftSets = hasCustomLiftSets,
            onCustomLiftSetsToggled = onCustomLiftSetsToggled,
            onReplaceLift = onReplaceLift,
            onReplaceMovementPattern = onReplaceMovementPattern,
        )
    }
}

@Composable
fun StandardSettings(
    isVisible: Boolean,
    setCount: Int,
    repRangeBottom: Int,
    repRangeTop: Int,
    rpeTarget: Double,
    progressionScheme: ProgressionScheme,
    onToggleRpePicker: (Boolean) -> Unit,
    onSetCountChanged: (Int) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRpeTargetChanged: (Double) -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            expandFrom = Alignment.Bottom,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Bottom,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IntegerOnlyTextField(modifier = Modifier.weight(1f), maxValue = 10, value = setCount, label = "Sets", onValueChanged = onSetCountChanged)
            Spacer(modifier = Modifier.width(2.dp))
            IntegerOnlyTextField(modifier = Modifier.weight(1f),value = repRangeBottom, label = "Rep Range Bottom", onValueChanged = onRepRangeBottomChanged)
            Spacer(modifier = Modifier.width(2.dp))
            IntegerOnlyTextField(modifier = Modifier.weight(1f),value = repRangeTop, label = "Rep Range Top", onValueChanged = onRepRangeTopChanged)
            Spacer(modifier = Modifier.width(2.dp))
            DoubleTextField(modifier = Modifier.weight(1f),
                value = rpeTarget,
                disableKeyboard = true,
                onFocusChanged = onToggleRpePicker,
                label = when(progressionScheme) {
                    ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "RPE"
                    ProgressionScheme.LINEAR_PROGRESSION -> "Max RPE"
                    else -> "Top Set RPE"
                },
                onValueChanged = onRpeTargetChanged
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
fun CustomSettings(
    isVisible: Boolean,
    customSets: List<GenericCustomLiftSet>,
    onAddSet: () -> Unit,
    onSetMatchingChanged: (position: Int, enabled: Boolean) -> Unit,
    onMatchSetGoalChanged: (position: Int, newMatchSetGoal: Int) -> Unit,
    onRepRangeBottomChanged: (position: Int, newRepRangeBottom: Int) -> Unit,
    onRepRangeTopChanged: (position: Int, newRepRangeTop: Int) -> Unit,
    onRpeTargetChanged: (position: Int, newRpeTarget: Double) -> Unit,
    onRepFloorChanged: (position: Int, newRepFloor: Int) -> Unit,
    onDropPercentageChanged: (position: Int, newDropPercentage: Double) -> Unit,
    onCustomSetTypeChanged: (position: Int, newSetType: SetType) -> Unit,
    toggleRpePicker: (position: Int, visible: Boolean) -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        )
    ) {
        Column {
            var prevSetType: SetType? = null
            customSets.forEachIndexed { index, set ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(5.dp))
                }

                when (set) {
                    is StandardSetDto -> {
                        StandardSet(
                            position = set.position,
                            useLabel = set.position == 0 || prevSetType != SetType.STANDARD_SET,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            onRpeTargetChanged = { onRpeTargetChanged(set.position, it) },
                            onRepRangeBottomChanged = { onRepRangeBottomChanged(set.position, it) },
                            onRepRangeTopChanged = { onRepRangeTopChanged(set.position, it) },
                            onCustomSetTypeChanged = { onCustomSetTypeChanged(set.position, it) },
                            toggleRpePicker = { toggleRpePicker(set.position, it) },
                        )
                        prevSetType = SetType.STANDARD_SET
                    }
                    is MyoRepSetDto -> {
                        MyoRepSet(
                            position = set.position,
                            repFloor = set.repFloor,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            setMatching = set.setMatching,
                            matchSetGoal = set.matchSetGoal,
                            onSetMatchingChanged = { onSetMatchingChanged(set.position, it) },
                            onRepFloorChanged = { onRepFloorChanged(set.position, it) },
                            onRepRangeBottomChanged = { onRepRangeBottomChanged(set.position, it) },
                            onRepRangeTopChanged = { onRepRangeTopChanged(set.position, it) },
                            onCustomSetTypeChanged = { onCustomSetTypeChanged(set.position, it) },
                            onMatchSetGoalChanged = { onMatchSetGoalChanged(set.position, it) },
                        )
                        prevSetType = SetType.MYOREP_SET
                    }
                    is DropSetDto -> {
                        DropSet(
                            position = set.position,
                            useLabel = set.position == 0 || prevSetType != SetType.DROP_SET,
                            dropPercentage = set.dropPercentage,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            onDropPercentageChanged = { /* TODO */ },
                            onRpeTargetChanged = { onRpeTargetChanged(set.position, it) },
                            onRepRangeBottomChanged = { onRepRangeBottomChanged(set.position, it) },
                            onRepRangeTopChanged = { onRepRangeTopChanged(set.position, it) },
                            onCustomSetTypeChanged = { onCustomSetTypeChanged(set.position, it) },
                            toggleRpePicker = { toggleRpePicker(set.position, it) },
                        )
                        prevSetType = SetType.DROP_SET
                    }
                }
            }
            Spacer(modifier = Modifier.height(25.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.clickable { onAddSet() },
                    text = "Add Set",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun StandardSet(
    position: Int,
    useLabel: Boolean,
    rpeTarget: Double,
    repRangeBottom: Int,
    repRangeTop: Int,
    onRpeTargetChanged: (Double) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onCustomSetTypeChanged: (SetType) -> Unit,
    toggleRpePicker: (Boolean) -> Unit
) {
    var repRangeBottomLabel by remember { mutableStateOf(if (useLabel) "Rep Range Bottom" else "") }
    var repRangeTopLabel by remember { mutableStateOf(if (useLabel) "Rep Range Top" else "") }
    var rpeLabel by remember { mutableStateOf(if(useLabel) "RPE" else "") }

    LaunchedEffect(useLabel) {
        repRangeBottomLabel = if (useLabel) "Rep Range Bottom" else ""
        repRangeTopLabel = if (useLabel) "Rep Range Top" else ""
        rpeLabel = if(useLabel) "RPE" else ""
    }

    if (useLabel) {
        Spacer(modifier = Modifier.height(10.dp))
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        Box (modifier = Modifier.offset(y = if(useLabel) 6.dp else 0.dp)) {
            CustomSetTypeDropdown(text = position.toString(), standardShortDisplayName = position.toString(), onCustomSetTypeChanged = onCustomSetTypeChanged)
        }
        Spacer(modifier = Modifier.width(5.dp))
        IntegerOnlyTextField(modifier = Modifier.weight(1f), value = repRangeBottom, label = repRangeBottomLabel, onValueChanged = onRepRangeBottomChanged)
        Spacer(modifier = Modifier.width(2.dp))
        IntegerOnlyTextField(modifier = Modifier.weight(1f), value = repRangeTop, label = repRangeTopLabel, onValueChanged = onRepRangeTopChanged)
        Spacer(modifier = Modifier.width(2.dp))
        DoubleTextField(
            modifier = Modifier.weight(1f),
            disableKeyboard = true,
            value = rpeTarget,
            label = rpeLabel,
            onValueChanged = onRpeTargetChanged,
            onFocusChanged = toggleRpePicker
        )
        Spacer(modifier = Modifier.width(10.dp))
    }
}

@Composable
fun MyoRepSet(
    position: Int,
    repFloor: Int,
    repRangeBottom: Int,
    repRangeTop: Int,
    setMatching: Boolean,
    matchSetGoal: Int?,
    onSetMatchingChanged: (enabled: Boolean) -> Unit,
    onMatchSetGoalChanged: (Int) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRepFloorChanged: (Int) -> Unit,
    onCustomSetTypeChanged: (SetType) -> Unit,
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    var repRangeTopLabel by remember { mutableStateOf(if(!setMatching) "Activation Set Rep Range Top" else "Activation Set Reps") }
    val myoRepSetDisplayNameDisplayNameShort by remember { mutableStateOf(SetType.MYOREP_SET.displayNameShort()) }
    val myoRepSetDisplayNameDisplayName by remember { mutableStateOf(SetType.MYOREP_SET.displayName()) }

    LaunchedEffect(setMatching) {
        repRangeTopLabel = if(!setMatching) "Activation Set Rep Range Top" else "Activation Set Reps"
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        if (!detailsExpanded) {
            CustomSetTypeDropdown(
                text = myoRepSetDisplayNameDisplayNameShort,
                standardShortDisplayName = position.toString(),
                onCustomSetTypeChanged = onCustomSetTypeChanged
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        CustomSetExpandableCard(
            isCollapsed = !detailsExpanded,
            leftSideSummaryText = "Top Set $repRangeBottom - $repRangeTop reps",
            centerIconResourceId = R.drawable.descend_icon,
            rightSideSummaryText = "$repFloor reps",
            toggleExpansion = { detailsExpanded = !detailsExpanded },
            headerContent = {
                CustomSetTypeDropdown(
                    modifier = Modifier.padding(0.dp),
                    text = myoRepSetDisplayNameDisplayName,
                    fontSize = 18.sp,
                    standardShortDisplayName = position.toString(),
                    onCustomSetTypeChanged = onCustomSetTypeChanged
                )
            }
        ) {
            Column {
                LabeledCheckBox(
                    label = "Use Set Matching",
                    checked = setMatching,
                    onCheckedChanged = onSetMatchingChanged
                )
                if (!setMatching) {
                    IntegerOnlyTextField(
                        vertical = false,
                        value = repRangeBottom,
                        label = "Activation Set Rep Range Bottom",
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        labelFontSize = 14.sp,
                        onValueChanged = onRepRangeBottomChanged,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                IntegerOnlyTextField(
                    vertical = false,
                    value = repRangeTop,
                    label = repRangeTopLabel,
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    labelFontSize = 14.sp,
                    onValueChanged = onRepRangeTopChanged
                )
                Spacer(modifier = Modifier.width(2.dp))

                if(!setMatching) {
                    IntegerOnlyTextField(
                        vertical = false,
                        value = repFloor,
                        label = "Rep Floor",
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        labelFontSize = 14.sp,
                        onValueChanged = onRepFloorChanged
                    )
                }
                else {
                    IntegerOnlyTextField(
                        vertical = false,
                        value = matchSetGoal!!,
                        label = "Match Set Goal",
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        labelFontSize = 14.sp,
                        onValueChanged = onMatchSetGoalChanged
                    )
                }
            }
        }
    }
}

@Composable
fun DropSet(
    position: Int,
    useLabel: Boolean,
    dropPercentage: Double,
    rpeTarget: Double,
    repRangeBottom: Int?,
    repRangeTop: Int?,
    onDropPercentageChanged: (Boolean) -> Unit,
    onRpeTargetChanged: (Double) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onCustomSetTypeChanged: (SetType) -> Unit,
    toggleRpePicker: (Boolean) -> Unit,
) {
    var repRangeBottomLabel by remember { mutableStateOf(if (useLabel) "Rep Range Bottom" else "") }
    var repRangeTopLabel by remember { mutableStateOf(if (useLabel) "Rep Range Top" else "") }
    var rpeLabel by remember { mutableStateOf(if(useLabel) "RPE" else "") }

    LaunchedEffect(useLabel) {
        repRangeBottomLabel = if (useLabel) "Rep Range Bottom" else ""
        repRangeTopLabel = if (useLabel) "Rep Range Top" else ""
        rpeLabel = if(useLabel) "RPE" else ""
    }

    if (useLabel) {
        Spacer(modifier = Modifier.height(10.dp))
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CustomSetTypeDropdown(text = SetType.DROP_SET.displayNameShort(), standardShortDisplayName = position.toString(), onCustomSetTypeChanged = onCustomSetTypeChanged)
        Spacer(modifier = Modifier.width(5.dp))
        // TODO come up with way to pick drop percentage
        if (repRangeBottom != null) {
            IntegerOnlyTextField(modifier = Modifier.weight(1f), value = repRangeBottom, label = repRangeBottomLabel, onValueChanged = onRepRangeBottomChanged)
            Spacer(modifier = Modifier.width(2.dp))
        }
        if(repRangeTop != null){
            IntegerOnlyTextField(modifier = Modifier.weight(1f), value = repRangeTop, label = repRangeTopLabel, onValueChanged = onRepRangeTopChanged)
            Spacer(modifier = Modifier.width(2.dp))
        }
        if (repRangeBottom != null && repRangeTop != null) {
            DoubleTextField(
                modifier = Modifier.weight(1f),
                disableKeyboard = true,
                value = rpeTarget,
                label = rpeLabel,
                onValueChanged = onRpeTargetChanged,
                onFocusChanged = toggleRpePicker,
            )
        }
    }
}

@Composable
fun CustomSetContainer(
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .padding(0.dp, 0.dp, 10.dp, 0.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
    ) {
        content()
    }
}

@Composable
fun CustomSetExpandableCard(
    isCollapsed: Boolean,
    leftSideSummaryText: String,
    centerIconResourceId: Int,
    rightSideSummaryText: String,
    toggleExpansion: () -> Unit,
    headerContent: @Composable (BoxScope.() -> Unit),
    detailContent: @Composable (BoxScope.() -> Unit),
) {
    CustomSetContainer(toggleExpansion) {
        Box(modifier = Modifier.animateContentSize()) {
            if (isCollapsed) {
                CustomSetSummary(
                    leftSideSummaryText = leftSideSummaryText,
                    centerIconResourceId = centerIconResourceId,
                    rightSideSummaryText = rightSideSummaryText
                )
            } else {
                CustomSetDetails(headerContent = headerContent) {
                    detailContent()
                }
            }
        }
    }
}

@Composable
fun CustomSetSummary(
    leftSideSummaryText: String,
    centerIconResourceId: Int,
    rightSideSummaryText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = leftSideSummaryText,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = centerIconResourceId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = rightSideSummaryText,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun CustomSetDetails(
    headerContent: @Composable (BoxScope.() -> Unit),
    detailContent: @Composable (BoxScope.() -> Unit),
) {
    Column {
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            headerContent()
        }
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            detailContent()
        }
    }
}

@Composable
fun LiftDropdown(
    hasCustomLiftSets: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceMovementPattern: () -> Unit,
    onReplaceLift: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var dropdownExpanded by remember { mutableStateOf(false) }
    var customLiftsEnabled by remember { mutableStateOf(hasCustomLiftSets) }

    IconDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = dropdownExpanded,
        onToggleExpansion = { dropdownExpanded = !dropdownExpanded }
    ) {
        DropdownMenuItem(
            text = { Text("Replace Movement Pattern") },
            onClick = {
                dropdownExpanded = false
                onReplaceMovementPattern.invoke()
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Replace Lift") },
            onClick = {
                dropdownExpanded = false
                onReplaceLift.invoke()
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Custom Sets") },
            onClick = {
                customLiftsEnabled = !customLiftsEnabled
                coroutineScope.launch{
                    delay(100)
                    dropdownExpanded = false
                    onCustomLiftSetsToggled(customLiftsEnabled)
                }
            },
            leadingIcon = {
                Switch(
                    enabled = true,
                    checked = customLiftsEnabled,
                    onCheckedChange = {
                        customLiftsEnabled = it
                        coroutineScope.launch{
                            delay(100)
                            dropdownExpanded = false
                            onCustomLiftSetsToggled(it)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedIconColor = MaterialTheme.colorScheme.onTertiary,
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    )
                )
            }
        )
    }
}

@Composable
fun ProgressionSchemeDropdown(
    modifier: Modifier = Modifier,
    text: String,
    onChangeProgressionScheme: (ProgressionScheme) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val progressionSchemes by remember { mutableStateOf(ProgressionScheme.values().sortedBy { it.displayName() }) }

    Row(
        modifier = Modifier.animateContentSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier.width(10.dp))
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = R.drawable.three_bars),
            tint = MaterialTheme.colorScheme.outline,
            contentDescription = null
        )
        Spacer(modifier.width(4.dp))
        TextDropdown(
            isExpanded = isExpanded,
            onToggleExpansion = { isExpanded = !isExpanded },
            text = text,
            fontSize = 18.sp
        ) {
            for (progressionScheme in progressionSchemes) {
                DropdownMenuItem(
                    text = { Text(progressionScheme.displayName()) },
                    onClick = {
                        isExpanded = false
                        onChangeProgressionScheme(progressionScheme)
                    },
                    leadingIcon = {
                        TextDropdownTextAnchor(
                            text = progressionScheme.displayNameShort(),
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun CustomSetTypeDropdown(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 14.sp,
    standardShortDisplayName: String,
    onCustomSetTypeChanged: (newSetType: SetType) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val setTypes by remember { mutableStateOf(SetType.values().sortedBy { it.displayName() }) }

    TextDropdown(
        modifier = modifier,
        isExpanded = isExpanded,
        onToggleExpansion = { isExpanded = !isExpanded },
        text = text,
        fontSize = fontSize
    ) {
        for (setType in setTypes) {
            DropdownMenuItem(
                text = { Text(setType.displayName()) },
                onClick = {
                    isExpanded = false
                    onCustomSetTypeChanged(setType)
                },
                leadingIcon = {
                    TextDropdownTextAnchor(
                        text = setType.displayNameShort(standardShortDisplayName),
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}
