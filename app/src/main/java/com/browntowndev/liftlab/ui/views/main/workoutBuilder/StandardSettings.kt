package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.ui.composables.text.FloatTextField
import com.browntowndev.liftlab.ui.composables.text.IntegerTextField
import com.browntowndev.liftlab.ui.extensions.rpeLabel


@Composable
fun StandardSettings(
    isVisible: Boolean,
    listState: LazyListState,
    setCount: Int,
    repRangeBottom: Int,
    repRangeTop: Int,
    rpeTarget: Float,
    volumeCyclingSetCeiling: Int?,
    progressionScheme: ProgressionScheme,
    onSetCountChanged: (Int) -> Unit,
    onVolumeCyclingSetCeilingChanged: (Int) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRpeTargetChanged: (Float?) -> Unit,
    onToggleRpePicker: (Boolean) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
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
        StandardSettingRow(
            listState = listState,
            setCount = setCount,
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            rpeTarget = rpeTarget,
            volumeCyclingSetCeiling = volumeCyclingSetCeiling,
            progressionScheme = progressionScheme,
            onSetCountChanged = onSetCountChanged,
            onVolumeCyclingSetCeilingChanged = onVolumeCyclingSetCeilingChanged,
            onRepRangeBottomChanged = onRepRangeBottomChanged,
            onRepRangeTopChanged = onRepRangeTopChanged,
            onRpeTargetChanged = onRpeTargetChanged,
            onToggleRpePicker = onToggleRpePicker,
            onPixelOverflowChanged = onPixelOverflowChanged,
        )
    }
}

@Composable
private fun StandardSettingRow(
    listState: LazyListState,
    setCount: Int,
    repRangeBottom: Int,
    repRangeTop: Int,
    rpeTarget: Float,
    volumeCyclingSetCeiling: Int?,
    progressionScheme: ProgressionScheme,
    onSetCountChanged: (Int) -> Unit,
    onVolumeCyclingSetCeilingChanged: (Int) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRpeTargetChanged: (Float?) -> Unit,
    onToggleRpePicker: (Boolean) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
) {
    Column (verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val setLabel =
                if (volumeCyclingSetCeiling != null) "Start Sets"
                else "Sets"

            IntegerTextField(
                modifier = Modifier.weight(1f),
                listState = listState,
                minValue = 1,
                maxValue = 10,
                value = setCount,
                emitOnlyOnLostFocus = true,
                label = setLabel,
                onNonNullValueChanged = onSetCountChanged,
            )
            Spacer(modifier = Modifier.width(2.dp))
            IntegerTextField(
                modifier = Modifier.weight(1f),
                listState = listState,
                value = repRangeBottom,
                emitOnlyOnLostFocus = true,
                minValue = 1,
                label = "Rep Range Bottom",
                onNonNullValueChanged = onRepRangeBottomChanged,
            )
            Spacer(modifier = Modifier.width(2.dp))
            IntegerTextField(
                modifier = Modifier.weight(1f),
                listState = listState,
                value = repRangeTop,
                emitOnlyOnLostFocus = true,
                minValue = 1,
                label = "Rep Range Top",
                onNonNullValueChanged = onRepRangeTopChanged,
            )
            if (progressionScheme != ProgressionScheme.TOP_SET_PROGRESSION) {
                Spacer(modifier = Modifier.width(2.dp))
                FloatTextField(
                    modifier = Modifier.weight(1f),
                    value = rpeTarget,
                    listState = listState,
                    disableSystemKeyboard = true,
                    hideCursor = true,
                    updateValueWhileFocused = true,
                    label = progressionScheme.rpeLabel(),
                    onFocusChanged = onToggleRpePicker,
                    onValueChanged = onRpeTargetChanged,
                    onPixelOverflowChanged = onPixelOverflowChanged,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        val volumeCyclingEnabled = volumeCyclingSetCeiling != null
        AnimatedVisibility(
            visible = volumeCyclingEnabled,
            enter = slideInVertically(
                initialOffsetY = { -it }, // Slide in from top
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            ) + expandVertically(expandFrom = Alignment.Top),
            exit = slideOutVertically(
                targetOffsetY = { -it }, // Slide out to top
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            ) + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IntegerTextField(
                    modifier = Modifier.weight(1f),
                    listState = listState,
                    minValue = 1,
                    maxValue = 10,
                    value = volumeCyclingSetCeiling,
                    emitOnlyOnLostFocus = true,
                    label = "End Sets",
                    onNonNullValueChanged = onVolumeCyclingSetCeilingChanged,
                )
                Spacer(modifier = Modifier.weight(3f)) // Add spacer to push the TextField to the left
            }
        }
    }
}
