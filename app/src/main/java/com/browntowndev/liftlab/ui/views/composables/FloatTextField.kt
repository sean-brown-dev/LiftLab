package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun FloatTextField(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    vertical: Boolean = true,
    maxValue: Float = 10f,
    minValue: Float = 0f,
    precision: Int = 2,
    fontSize: TextUnit = 18.sp,
    value: Float?,
    errorOnEmpty: Boolean = true,
    placeholder: String = "",
    label: String = "",
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    labelFontSize: TextUnit = 10.sp,
    disableSystemKeyboard: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onLeftFocusBlank: () -> Unit = {},
    onValueChanged: (Float) -> Unit = {},
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    val textNoDotZero = value?.toString()?.removeSuffix(".0") ?: ""

    ScrollableTextField(
        modifier = modifier,
        listState = listState,
        vertical = vertical,
        errorOnEmptyString = errorOnEmpty,
        fontSize = fontSize,
        value = textNoDotZero,
        placeholder = placeholder,
        label = label,
        labelColor = labelColor,
        labelFontSize = labelFontSize,
        disableSystemKeyboard = disableSystemKeyboard,
        onFocusChanged = onFocusChanged,
        onLeftFocusBlank = onLeftFocusBlank,
        onValueChanged = { newValue ->
            validateFloat(
                newValue = newValue,
                existingValue = textNoDotZero,
                maxValue = maxValue,
                minValue = minValue,
                precision = precision,
                onValueChanged = onValueChanged
            )
        },
        onPixelOverflowChanged = onPixelOverflowChanged,
    )
}

private fun validateFloat(
    newValue: String,
    existingValue: String,
    maxValue: Float,
    minValue: Float,
    precision: Int,
    onValueChanged: (Float) -> Unit,
): String {
    var newValueAsNumber = newValue.trim().toFloatOrNull()
    var text: String = existingValue

    if (newValueAsNumber != null) {
        if (newValueAsNumber <= maxValue && newValue.substringAfter('.', "").length <= precision) {
            newValueAsNumber = if (newValueAsNumber >= minValue) newValueAsNumber else minValue
            text = newValueAsNumber.toString()
            onValueChanged(newValueAsNumber)
        }
    } else if (
        newValue.isEmpty() ||
        (newValue.last() == '.' && newValue.count { it == '.' } == 1)
    ) {
        text = newValue
    }

    return text.removeSuffix(".0")
}
