package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.ui.views.utils.DoubleTextField
import com.browntowndev.liftlab.ui.views.utils.IntegerTextField


@Composable
fun StandardSettings(
    isVisible: Boolean,
    listState: LazyListState,
    setCount: Int,
    repRangeBottom: Int,
    repRangeTop: Int,
    rpeTarget: Double,
    progressionScheme: ProgressionScheme,
    onSetCountChanged: (Int) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRpeTargetChanged: (Double) -> Unit,
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
            progressionScheme = progressionScheme,
            onSetCountChanged = onSetCountChanged,
            onRepRangeBottomChanged = onRepRangeBottomChanged,
            onRepRangeTopChanged = onRepRangeTopChanged,
            onRpeTargetChanged = onRpeTargetChanged,
            onToggleRpePicker = onToggleRpePicker,
            onPixelOverflowChanged = onPixelOverflowChanged
        )
    }
}

@Composable
private fun StandardSettingRow(
    listState: LazyListState,
    setCount: Int,
    repRangeBottom: Int,
    repRangeTop: Int,
    rpeTarget: Double,
    progressionScheme: ProgressionScheme,
    onSetCountChanged: (Int) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRpeTargetChanged: (Double) -> Unit,
    onToggleRpePicker: (Boolean) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IntegerTextField(
            modifier = Modifier.weight(1f),
            listState = listState,
            maxValue = 10,
            value = setCount,
            label = "Sets",
            onValueChanged = onSetCountChanged
        )
        Spacer(modifier = Modifier.width(2.dp))
        IntegerTextField(
            modifier = Modifier.weight(1f),
            listState = listState,
            value = repRangeBottom,
            label = "Rep Range Bottom",
            onValueChanged = onRepRangeBottomChanged
        )
        Spacer(modifier = Modifier.width(2.dp))
        IntegerTextField(
            modifier = Modifier.weight(1f),
            listState = listState,
            value = repRangeTop,
            label = "Rep Range Top",
            onValueChanged = onRepRangeTopChanged
        )
        Spacer(modifier = Modifier.width(2.dp))
        DoubleTextField(
            modifier = Modifier.weight(1f),
            value = rpeTarget,
            listState = listState,
            disableSystemKeyboard = true,
            label = when (progressionScheme) {
                ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "RPE"
                ProgressionScheme.LINEAR_PROGRESSION -> "Max RPE"
                else -> "Top Set RPE"
            },
            onFocusChanged = onToggleRpePicker,
            onValueChanged = onRpeTargetChanged,
            onPixelOverflowChanged = onPixelOverflowChanged,
        )
        Spacer(modifier = Modifier.width(10.dp))
    }
}
