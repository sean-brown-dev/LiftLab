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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.insertSuperscript
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.ui.views.composables.CircledTextIcon
import com.browntowndev.liftlab.ui.views.composables.VolumeChipBottomSheet


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkoutPreview(
    paddingValues: PaddingValues,
    visible: Boolean,
    workoutInProgress: Boolean,
    workoutName: String,
    timeInProgress: String,
    lifts: List<LoggingWorkoutLiftDto>,
    volumeTypes: List<CharSequence>,
    showWorkoutLog: () -> Unit,
    startWorkout: () -> Unit,
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
            volumeChipLabels = volumeTypes
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
                    val hasMyoRepSets = remember { lift.sets.any { it is LoggingMyoRepSetDto } }
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
                                startWorkout()
                            } else {
                                showWorkoutLog()
                            }
                        }
                    ) {
                        var text by remember {
                            mutableStateOf(
                                if (!workoutInProgress) "Start $workoutName"
                                else "Resume - $timeInProgress"
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

                        LaunchedEffect(key1 = workoutInProgress, key2 = timeInProgress ) {
                            textColor = if (!workoutInProgress) onPrimaryColor
                            else onSecondaryColor

                            text = if (!workoutInProgress) "Start $workoutName"
                            else "Resume - $timeInProgress"
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
