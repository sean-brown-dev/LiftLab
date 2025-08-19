package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.browntowndev.liftlab.ui.composables.text.FloatTextField
import com.browntowndev.liftlab.ui.composables.text.IntegerTextField


@Composable
fun LoggableSet(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    animateVisibility: Boolean,
    animationEnabled: Boolean = true, // For tests
    isEdit: Boolean,
    position: Int,
    myoRepSetPosition: Int?,
    setNumberLabel: String,
    previousSetResultLabel: String,
    weightRecommendation: Float?,
    repRangePlaceholder: String,
    rpeTargetPlaceholder: String,
    complete: Boolean,
    completedWeight: Float?,
    completedReps: Int?,
    completedRpe: Float?,
    onWeightChanged: (weight: Float?) -> Unit,
    onRepsChanged: (reps: Int?) -> Unit,
    onCompleted: (weight: Float, reps: Int, rpe: Float) -> Unit,
    onUndoCompletion: () -> Unit,
    toggleRpePicker: (visible: Boolean) -> Unit,
    onAddSpacer: (height: Dp) -> Unit,
) {
    var hasAnimated by remember(position, myoRepSetPosition) { mutableStateOf(false) }
    val transitionState = remember(position, myoRepSetPosition, animateVisibility) {
        MutableTransitionState(!animateVisibility || !animationEnabled).apply { targetState = true }
    }

    // Only animate the first time the set becomes visible
    val enterTransition = if (!hasAnimated && animateVisibility && animationEnabled) {
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
        )
    } else {
        EnterTransition.None
    }

    LaunchedEffect(transitionState) {
        if (transitionState.currentState) hasAnimated = true
    }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = enterTransition,
        exit = ExitTransition.None,
    ) {
        SetRow(
            modifier = modifier,
            lazyListState = lazyListState,
            isEdit = isEdit,
            setNumberLabel = setNumberLabel,
            previousSetResultLabel = previousSetResultLabel,
            weightRecommendation = weightRecommendation,
            repRangePlaceholder = repRangePlaceholder,
            rpeTargetPlaceholder = rpeTargetPlaceholder,
            complete = complete,
            completedReps = completedReps,
            completedWeight = completedWeight,
            completedRpe = completedRpe,
            onWeightChanged = onWeightChanged,
            onRepsChanged = onRepsChanged,
            onCompleted = onCompleted,
            onUndoCompletion = onUndoCompletion,
            toggleRpePicker = toggleRpePicker,
            onAddSpacer = onAddSpacer,
        )
    }
}

@Composable
internal fun SetRow(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    isEdit: Boolean,
    setNumberLabel: String,
    previousSetResultLabel: String,
    weightRecommendation: Float?,
    repRangePlaceholder: String,
    rpeTargetPlaceholder: String,
    complete: Boolean,
    completedReps: Int?,
    completedWeight: Float?,
    completedRpe: Float?,
    onWeightChanged: (weight: Float?) -> Unit,
    onRepsChanged: (reps: Int?) -> Unit,
    onCompleted: (weight: Float, reps: Int, rpe: Float) -> Unit,
    onUndoCompletion: () -> Unit,
    toggleRpePicker: (visible: Boolean) -> Unit,
    onAddSpacer: (height: Dp) -> Unit,
) {
    Row(
        modifier = modifier.then(Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp)),
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
            errorOnEmpty = isEdit,
            emitOnlyOnLostFocus = true,
            maxValue = 2000f,
            onValueChanged = onWeightChanged,
        )
        Spacer(modifier = Modifier.width(8.dp))
        IntegerTextField(
            modifier = Modifier.weight(1f),
            value = completedReps,
            placeholder = repRangePlaceholder,
            errorOnEmpty = isEdit,
            emitOnlyOnLostFocus = true,
            onValueChanged = onRepsChanged,
        )
        Spacer(modifier = Modifier.width(8.dp))
        FloatTextField(
            modifier = Modifier.weight(1f),
            listState = lazyListState,
            value = completedRpe,
            placeholder = rpeTargetPlaceholder,
            disableSystemKeyboard = true,
            hideCursor = true,
            updateValueWhileFocused = true,
            errorOnEmpty = isEdit,
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
