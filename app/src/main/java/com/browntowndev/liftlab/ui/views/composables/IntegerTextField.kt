package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun IntegerTextField(
    modifier: Modifier = Modifier,
    placeholder: String = "",
    listState: LazyListState? = null,
    vertical: Boolean = true,
    maxValue: Int = 99,
    minValue: Int = 0,
    value: Int?,
    fontSize: TextUnit = 18.sp,
    errorOnEmpty: Boolean = true,
    label: String = "",
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    labelFontSize: TextUnit = 10.sp,
    disableSystemKeyboard: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onLeftFocusBlank: () -> Unit = {},
    onValueChanged: (Int?) -> Unit = {},
    onNonNullValueChanged: (Int) -> Unit = {},
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    ScrollableTextField(
        modifier = modifier,
        placeholder = placeholder,
        listState = listState,
        vertical = vertical,
        value = remember(value) { value?.toString() ?: "" },
        errorOnEmptyString = errorOnEmpty,
        label = label,
        fontSize = fontSize,
        labelColor = labelColor,
        labelFontSize = labelFontSize,
        disableSystemKeyboard = disableSystemKeyboard,
        onFocusChanged = onFocusChanged,
        onLeftFocusBlank = onLeftFocusBlank,
        onValueChanged = { newValue ->
            var newValueAsInt = newValue.trim().toIntOrNull()
            if (newValueAsInt != null && newValueAsInt <= maxValue) {
                newValueAsInt = if (newValueAsInt >= minValue) newValueAsInt else minValue
                onValueChanged(newValueAsInt)
                onNonNullValueChanged(newValueAsInt)
                newValueAsInt.toString()
            } else if (newValue.isEmpty()) {
                onValueChanged(null)
                newValue
            } else {
                ""
            }
        },
        onPixelOverflowChanged = onPixelOverflowChanged,
    )
}
