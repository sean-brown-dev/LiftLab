package com.browntowndev.liftlab.ui.views.utils

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
    value: Float?,
    errorOnEmpty: Boolean = true,
    placeholder: Float? = null,
    label: String = "",
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    labelFontSize: TextUnit = 10.sp,
    disableSystemKeyboard: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onValueChanged: (Float) -> Unit = {},
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    val textNoDotZero = value?.toString()?.removeSuffix(".0")?.ifEmpty { "0" } ?: ""

    ScrollableTextField(
        modifier = modifier,
        listState = listState,
        vertical = vertical,
        errorOnEmptyString = errorOnEmpty,
        value = textNoDotZero,
        placeholder = placeholder?.toString() ?: "",
        label = label,
        labelColor = labelColor,
        labelFontSize = labelFontSize,
        disableSystemKeyboard = disableSystemKeyboard,
        onFocusChanged = onFocusChanged,
        onValueChanged = { newValue ->
            validateFloat(
                newValue = newValue,
                existingValue = textNoDotZero,
                maxValue = maxValue,
                minValue = minValue,
                precision = precision,
                onValueChanged = { onValueChanged(it) }
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

    return text
}
