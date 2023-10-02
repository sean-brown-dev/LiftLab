package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.ui.views.composables.FloatTextField
import com.browntowndev.liftlab.ui.views.composables.IntegerTextField


@Composable
fun LoggableSet(
    index: Int,
    lazyListState: LazyListState,
    animateVisibility: Boolean,
    position: Int,
    progressionScheme: ProgressionScheme,
    setNumberLabel: String,
    previousSetResultLabel: String,
    weightRecommendation: Float?,
    repRangePlaceholder: String,
    complete: Boolean,
    completedReps: Int?,
    completedWeight: Float?,
    completedRpe: Float?,
    rpeTarget: Float,
    onCompleted: (weight: Float, reps: Int, rpe: Float) -> Unit,
    onUndoCompletion: () -> Unit,
    toggleRpePicker: (visible: Boolean) -> Unit,
    onAddSpacer: (height: Dp) -> Unit,
) {
    val transitionState = remember(animateVisibility) { MutableTransitionState(!animateVisibility) }
    AnimatedVisibility(
        visibleState = transitionState,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
        )
    ) {
        SetRow(
            index,
            lazyListState,
            position,
            progressionScheme,
            setNumberLabel,
            previousSetResultLabel,
            weightRecommendation,
            repRangePlaceholder,
            complete,
            completedReps,
            completedWeight,
            completedRpe,
            rpeTarget,
            onCompleted,
            onUndoCompletion,
            toggleRpePicker,
            onAddSpacer
        )
    }

    LaunchedEffect(animateVisibility) {
        if (animateVisibility) {
            transitionState.targetState = true
        }
    }
}

@Composable
private fun SetRow(
    index: Int,
    lazyListState: LazyListState,
    position: Int,
    progressionScheme: ProgressionScheme,
    setNumberLabel: String,
    previousSetResultLabel: String,
    weightRecommendation: Float?,
    repRangePlaceholder: String,
    complete: Boolean,
    completedReps: Int?,
    completedWeight: Float?,
    completedRpe: Float?,
    rpeTarget: Float,
    onCompleted: (weight: Float, reps: Int, rpe: Float) -> Unit,
    onUndoCompletion: () -> Unit,
    toggleRpePicker: (visible: Boolean) -> Unit,
    onAddSpacer: (height: Dp) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(.25f),
            text = setNumberLabel,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = previousSetResultLabel,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.width(8.dp))
        var weight: Float? by remember(key1 = index, key2 = completedWeight) { mutableStateOf(completedWeight) }
        FloatTextField(
            modifier = Modifier.weight(1f),
            listState = lazyListState,
            value = weight,
            placeholder = weightRecommendation?.toString()?.removeSuffix(".0") ?: "",
            errorOnEmpty = false,
            maxValue = Float.MAX_VALUE,
            onValueChanged = {
                weight = it
                if (complete) {
                    onCompleted(weight!!, completedReps!!, completedRpe!!)
                }
            },
            onLeftFocusBlank = {
                weight = null
                if (complete) {
                    onUndoCompletion()
                }
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        var reps: Int? by remember(key1 = index, key2 = completedReps) { mutableStateOf(completedReps) }
        IntegerTextField(
            modifier = Modifier.weight(1f),
            value = reps,
            placeholder = repRangePlaceholder,
            errorOnEmpty = false,
            onValueChanged = {
                reps = it
                if (complete) {
                    onCompleted(completedWeight!!, reps!!, completedRpe!!)
                }
            },
            onLeftFocusBlank = {
                reps = null
                if (complete) {
                    onUndoCompletion()
                }
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        var rpe: Float? by remember(key1 = index, key2 = completedRpe) { mutableStateOf(completedRpe) }
        val rpePlaceholder = remember(key1 = index, key2 = rpeTarget) {
            if (position == 0) {
                rpeTarget.toString().removeSuffix(".0")
            } else {
                when (progressionScheme) {
                    ProgressionScheme.DOUBLE_PROGRESSION,
                    ProgressionScheme.WAVE_LOADING_PROGRESSION -> ""

                    ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> rpeTarget.toString().removeSuffix(".0")

                    ProgressionScheme.LINEAR_PROGRESSION -> "≤${rpeTarget.toString().removeSuffix(".0")}"
                }
            }
        }
        FloatTextField(
            modifier = Modifier.weight(1f),
            listState = lazyListState,
            value = rpe,
            placeholder = rpePlaceholder,
            disableSystemKeyboard = true,
            errorOnEmpty = false,
            onValueChanged = {
                rpe = it
                if (complete) {
                    onCompleted(completedWeight!!, completedReps!!, rpe!!)
                }
            },
            onFocusChanged = { toggleRpePicker(it) },
            onPixelOverflowChanged = onAddSpacer,
        )
        Spacer(modifier = Modifier.width(8.dp))
        val enabled by remember(
            key1 = index,
            key2 = weight != null && reps != null && rpe != null
        ) {
            mutableStateOf(weight != null && reps != null && rpe != null)
        }
        var checked by remember(key1 = index, key2 = complete) { mutableStateOf(complete) }
        LaunchedEffect(enabled) {
            if (!enabled && checked) {
                onUndoCompletion()
            }
        }
        Checkbox(
            checked = checked,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
            onCheckedChange = {
                checked = it
                if (it) {
                    onCompleted(weight!!, reps!!, rpe!!)
                } else {
                    onUndoCompletion()
                }
            }
        )
    }
}
