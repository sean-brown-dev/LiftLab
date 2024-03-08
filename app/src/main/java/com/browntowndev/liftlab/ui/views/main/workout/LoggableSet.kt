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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.ui.composables.FloatTextField
import com.browntowndev.liftlab.ui.composables.IntegerTextField


@Composable
fun LoggableSet(
    lazyListState: LazyListState,
    animateVisibility: Boolean,
    position: Int,
    progressionScheme: ProgressionScheme,
    setNumberLabel: String,
    previousSetResultLabel: String,
    weightRecommendation: Float?,
    repRangePlaceholder: String,
    complete: Boolean,
    completedWeight: Float?,
    completedReps: Int?,
    completedRpe: Float?,
    rpeTarget: Float,
    onWeightChanged: (weight: Float?) -> Unit,
    onRepsChanged: (reps: Int?) -> Unit,
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
            lazyListState = lazyListState,
            position = position,
            progressionScheme = progressionScheme,
            setNumberLabel = setNumberLabel,
            previousSetResultLabel = previousSetResultLabel,
            weightRecommendation = weightRecommendation,
            repRangePlaceholder = repRangePlaceholder,
            complete = complete,
            completedReps = completedReps,
            completedWeight = completedWeight,
            completedRpe = completedRpe,
            rpeTarget = rpeTarget,
            onWeightChanged = onWeightChanged,
            onRepsChanged = onRepsChanged,
            onCompleted = onCompleted,
            onUndoCompletion = onUndoCompletion,
            toggleRpePicker = toggleRpePicker,
            onAddSpacer = onAddSpacer,
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
    onWeightChanged: (weight: Float?) -> Unit,
    onRepsChanged: (reps: Int?) -> Unit,
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
        FloatTextField(
            modifier = Modifier.weight(1f),
            listState = lazyListState,
            value = completedWeight,
            placeholder = weightRecommendation?.toString()?.removeSuffix(".0") ?: "",
            errorOnEmpty = false,
            maxValue = 2000f,
            onValueChanged = onWeightChanged,
        )
        Spacer(modifier = Modifier.width(8.dp))
        IntegerTextField(
            modifier = Modifier.weight(1f),
            value = completedReps,
            placeholder = repRangePlaceholder,
            errorOnEmpty = false,
            onValueChanged = onRepsChanged,
        )
        Spacer(modifier = Modifier.width(8.dp))
        val rpePlaceholder = remember(rpeTarget) {
            if (position == 0) {
                rpeTarget.toString().removeSuffix(".0")
            } else {
                when (progressionScheme) {
                    ProgressionScheme.WAVE_LOADING_PROGRESSION -> ""
                    ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                    ProgressionScheme.DOUBLE_PROGRESSION -> rpeTarget.toString().removeSuffix(".0")
                    ProgressionScheme.LINEAR_PROGRESSION -> "≤${rpeTarget.toString().removeSuffix(".0")}"
                }
            }
        }
        FloatTextField(
            modifier = Modifier.weight(1f),
            listState = lazyListState,
            value = completedRpe,
            placeholder = rpePlaceholder,
            disableSystemKeyboard = true,
            hideCursor = true,
            errorOnEmpty = false,
            onFocusChanged = { toggleRpePicker(it) },
            onPixelOverflowChanged = onAddSpacer,
        )
        Spacer(modifier = Modifier.width(8.dp))
        val enabled by remember(
            key1 = completedWeight,
            key2 = completedReps,
            key3 = completedRpe
        ) {
            mutableStateOf(completedWeight != null && completedReps != null && completedRpe != null)
        }
        Checkbox(
            checked = complete,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
            onCheckedChange = {
                if (it) {
                    onCompleted(completedWeight!!, completedReps!!, completedRpe!!)
                } else if (complete) {
                    onUndoCompletion()
                }
            }
        )
    }
}
