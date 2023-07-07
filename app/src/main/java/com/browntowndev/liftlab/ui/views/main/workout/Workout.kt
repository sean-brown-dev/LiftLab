package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.navigation.NavHostController
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.insertSuperscript
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.ui.models.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.TimerViewModel
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutScreen.Companion.REST_TIMER
import com.browntowndev.liftlab.ui.views.utils.CircledTextIcon
import com.browntowndev.liftlab.ui.views.utils.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.utils.FloatTextField
import com.browntowndev.liftlab.ui.views.utils.IntegerTextField
import com.browntowndev.liftlab.ui.views.utils.VolumeChipBottomSheet
import org.koin.androidx.compose.koinViewModel

@Composable
fun Workout(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
    mutateTopAppBarControlValue: (AppBarMutateControlRequest<Either<String?, Pair<Long, Boolean>>>) -> Unit,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    setBottomNavBarVisibility: (visible: Boolean) -> Unit,
    setTopAppBarControlVisibility: (String, Boolean) -> Unit,
) {
    val timerViewModel: TimerViewModel = koinViewModel()
    val workoutViewModel: WorkoutViewModel = koinViewModel()
    val state by workoutViewModel.state.collectAsState()
    val timerState by timerViewModel.state.collectAsState()

    workoutViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = workoutViewModel)

    LaunchedEffect(state.workoutWithProgression) {
        if (state.workoutWithProgression != null) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.TITLE,
                    state.workoutWithProgression!!.workout.name.left()
                )
            )
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.SUBTITLE,
                    ("Mesocycle: ${state.programMetadata!!.currentMesocycle + 1} " +
                     "Microcycle: ${state.programMetadata!!.currentMicrocycle + 1}").left()
                )
            )
        }
    }

    LaunchedEffect(key1 = state.workoutLogVisible) {
        setTopAppBarCollapsed(state.workoutLogVisible)
        setBottomNavBarVisibility(!state.workoutLogVisible)
        setTopAppBarControlVisibility(Screen.NAVIGATION_ICON, state.workoutLogVisible)
        setTopAppBarControlVisibility(REST_TIMER, state.workoutLogVisible)

        if (state.workoutLogVisible) {
            timerViewModel.start() // This is smart and won't restart from 0
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.SUBTITLE,
                    "Duration: ${timerState.time}".left()
                )
            )
        } else if (state.programMetadata != null) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.SUBTITLE,
                    ("Mesocycle: ${state.programMetadata!!.currentMesocycle + 1} " +
                            "Microcycle: ${state.programMetadata!!.currentMicrocycle + 1}").left()
                )
            )
        }
    }

    // Update subtitle on every tick
    LaunchedEffect(key1 = timerState.time) {
        if (state.workoutLogVisible) {
            mutateTopAppBarControlValue(
                AppBarMutateControlRequest(
                    Screen.SUBTITLE,
                    "Duration: ${timerState.time}".left()
                )
            )
        }
    }

    if (state.workoutWithProgression != null) {
        WorkoutPreview(
            paddingValues = paddingValues,
            visible = !state.workoutLogVisible,
            workoutInProgress = state.inProgress,
            workoutName = state.workoutWithProgression!!.workout.name,
            lifts = state.workoutWithProgression!!.workout.lifts,
            volumeTypes = state.volumeTypes,
            setInProgress = { workoutViewModel.setInProgress(it) },
            showWorkoutLog = { workoutViewModel.setWorkoutLogVisibility(true) }
        )
        WorkoutLog(
            paddingValues = paddingValues,
            visible = state.workoutLogVisible,
            lifts = state.workoutWithProgression!!.workout.lifts,
            completedSets = state.completedSets,
            progressions = state.workoutWithProgression!!.progressions,
            startRestTimer = {
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(REST_TIMER, Pair(it, true).right())
                )
            },
            cancelRestTimer = {
                mutateTopAppBarControlValue(
                    AppBarMutateControlRequest(REST_TIMER, Pair(0L, false).right())
                )
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkoutPreview(
    paddingValues: PaddingValues,
    visible: Boolean,
    workoutInProgress: Boolean,
    workoutName: String,
    lifts: List<GenericWorkoutLift>,
    volumeTypes: List<CharSequence>,
    showWorkoutLog: () -> Unit,
    setInProgress: (inProgress: Boolean) -> Unit,
) {
    AnimatedVisibility(
        modifier = Modifier.animateContentSize(),
        visible = visible,
        enter = scaleIn(initialScale = .6f, animationSpec = tween(durationMillis = 250, easing = LinearEasing)) + fadeIn(),
        exit = scaleOut(targetScale = .6f, animationSpec = tween(durationMillis = 250, easing = LinearEasing)) + fadeOut(),
    ) {
        VolumeChipBottomSheet(
            placeAboveBottomNavBar = true,
            title = "Workout Volume",
            volumeTypes
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
            ) {
                item {
                    Spacer(modifier = Modifier.height(5.dp))
                }
                items(lifts) { lift ->
                    val movementPatternDisplayName =
                        remember { lift.liftMovementPattern.displayName() }
                    val liftFirstLetter = remember { lift.liftName[0].toString() }
                    val hasMyoRepSets =
                        remember { (lift as? CustomWorkoutLiftDto)?.customLiftSets?.fastAny { it is MyoRepSetDto } }
                            ?: false
                    val liftNameWithSetCount = remember {
                        val setCountAndName = "${lift.setCount} x ${lift.liftName}"
                        if (hasMyoRepSets) setCountAndName.insertSuperscript(
                            "+myo",
                            lift.setCount.toString().length - 1
                        )
                        else setCountAndName
                    }
                    ListItem(
                        headlineContent = { Text(movementPatternDisplayName, fontSize = 20.sp) },
                        supportingContent = {
                            if (liftNameWithSetCount is AnnotatedString)
                                Text(liftNameWithSetCount, fontSize = 15.sp)
                            else if (liftNameWithSetCount is String)
                                Text(liftNameWithSetCount, fontSize = 15.sp)
                        },
                        leadingContent = { CircledTextIcon(text = liftFirstLetter) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background,
                            headlineColor = MaterialTheme.colorScheme.onBackground,
                            supportingColor = MaterialTheme.colorScheme.onBackground,
                            leadingIconColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                item {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    var buttonColor by remember {
                        mutableStateOf(
                            if (!workoutInProgress) primaryColor
                            else secondaryColor
                        )
                    }
                    LaunchedEffect(key1 = workoutInProgress) {
                        buttonColor = if (!workoutInProgress) primaryColor
                        else secondaryColor
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                        ),
                        onClick = {
                            if (!workoutInProgress) {
                                setInProgress(true)
                            } else {
                                showWorkoutLog()
                            }
                        }
                    ) {
                        var text by remember {
                            mutableStateOf(
                                if (!workoutInProgress) "Start $workoutName"
                                else "Resume"
                            )
                        }

                        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                        val onSecondaryColor = MaterialTheme.colorScheme.onSecondary
                        var textColor by remember {
                            mutableStateOf(
                                if (!workoutInProgress) onPrimaryColor
                                else onSecondaryColor
                            )
                        }

                        LaunchedEffect(key1 = workoutInProgress) {
                            textColor = if (!workoutInProgress) onPrimaryColor
                            else onSecondaryColor

                            text = if (!workoutInProgress) "Start $workoutName"
                            else "Resume"
                        }

                        Text(
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkoutLog(
    paddingValues: PaddingValues,
    visible: Boolean,
    lifts: List<GenericWorkoutLift>,
    completedSets: Map<String, SetResult>,
    progressions: Map<Long, List<ProgressionDto>>,
    startRestTimer: (time: Long) -> Unit,
    cancelRestTimer: () -> Unit,
) {
    AnimatedVisibility(
        modifier = Modifier.animateContentSize(),
        visible = visible,
        enter = scaleIn(initialScale = .6f, animationSpec = tween(durationMillis = 250, easing = LinearEasing)) + fadeIn(),
        exit = scaleOut(targetScale = .6f, animationSpec = tween(durationMillis = 250, easing = LinearEasing)) + fadeOut(),
    ) {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = lazyListState,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
                        Text(
                            modifier = Modifier.padding(10.dp),
                            text = lift.liftName,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        when (lift) {
                            is StandardWorkoutLiftDto -> {
                                for (i in 0 until lift.setCount) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        var completedSet by remember {
                                            mutableStateOf(
                                                completedSets["$lift.liftId-$i"]
                                            )
                                        }
                                        val weightRecommendation = remember {
                                            progressions[lift.id]
                                                ?.firstOrNull { it.setPosition == i }
                                                ?.weightRecommendation
                                        }
                                        LaunchedEffect(key1 = completedSets) {
                                            completedSet = completedSets["$lift.liftId-$i"]
                                        }

                                        val padding = remember { if(i == 0) 10.dp else 0.dp }
                                        Text(
                                            modifier = Modifier.padding(top = padding),
                                            text = (i + 1).toString(),
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        FloatTextField(
                                            modifier = Modifier.weight(1f),
                                            label = if (i == 0) "Weight" else "",
                                            listState = lazyListState,
                                            value = completedSet?.weight,
                                            placeholder = weightRecommendation,
                                            errorOnEmpty = false,
                                            maxValue = Float.MAX_VALUE,
                                            onValueChanged = { },
                                            onFocusChanged = { },
                                            onPixelOverflowChanged = { },
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IntegerTextField(
                                            modifier = Modifier.weight(1f),
                                            label = if (i == 0) "Reps" else "",
                                            value = completedSet?.reps,
                                            placeholder = "${lift.repRangeBottom}-${lift.repRangeTop}",
                                            errorOnEmpty = false,
                                            onValueChanged = { },
                                            onFocusChanged = { },
                                            onPixelOverflowChanged = { },
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IntegerTextField(
                                            modifier = Modifier.weight(1f),
                                            label = if (i == 0) "RPE" else "",
                                            value = completedSet?.reps,
                                            placeholder = lift.rpeTarget.toString().removeSuffix(".0"),
                                            disableSystemKeyboard = true,
                                            errorOnEmpty = false,
                                            onValueChanged = { },
                                            onFocusChanged = { },
                                            onPixelOverflowChanged = { },
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column (
                                            modifier = Modifier.fillMaxHeight(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Top,
                                        ) {
                                            if (i == 0) {
                                                Icon(
                                                    modifier = Modifier.size(14.dp),
                                                    imageVector = Icons.Filled.Check,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    contentDescription = null,
                                                )
                                            }
                                            Checkbox(
                                                checked = completedSet != null,
                                                enabled = true,
                                                colors = CheckboxDefaults.colors(
                                                    uncheckedColor = MaterialTheme.colorScheme.outline,
                                                    checkedColor = MaterialTheme.colorScheme.primary,
                                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                                ),
                                                onCheckedChange = { }
                                            )
                                        }
                                    }
                                }
                            }

                            is CustomWorkoutLiftDto -> {

                            }

                            else -> throw Exception("${lift::class.simpleName} is not defined.")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
            item {
                Button(onClick = { startRestTimer(3000L) }) {
                    Text("Show Rest Timer")
                }
            }
        }
    }
}
