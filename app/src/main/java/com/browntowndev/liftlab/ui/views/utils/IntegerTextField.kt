package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IntegerTextField(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    vertical: Boolean = true,
    maxValue: Int = 99,
    minValue: Int = 0,
    value: Int,
    label: String = "",
    labelColor: Color = MaterialTheme.colorScheme.outline,
    labelFontSize: TextUnit = 10.sp,
    disableSystemKeyboard: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onValueChanged: (Int) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    ScrollableTextField(
        modifier = modifier,
        listState = listState,
        vertical = vertical,
        value = value.toString(),
        label = label,
        labelColor = labelColor,
        labelFontSize = labelFontSize,
        disableSystemKeyboard = disableSystemKeyboard,
        onFocusChanged = onFocusChanged,
        onValueChanged = { newValue ->
            var newValueAsInt = newValue.trim().toIntOrNull()
            if (newValueAsInt != null && newValueAsInt <= maxValue) {
                newValueAsInt = if (newValueAsInt >= minValue) newValueAsInt else minValue
                onValueChanged(newValueAsInt)
                newValueAsInt.toString()
            } else if (newValue.isEmpty()) {
                newValue
            } else {
                value.toString()
            }
        },
        onPixelOverflowChanged = onPixelOverflowChanged,
    )
}