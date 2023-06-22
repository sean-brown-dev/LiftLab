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
fun DoubleTextField(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    vertical: Boolean = true,
    maxValue: Double = 10.0,
    minValue: Double = 0.0,
    precision: Int = 2,
    value: Double,
    label: String = "",
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    labelFontSize: TextUnit = 10.sp,
    disableSystemKeyboard: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onValueChanged: (Double) -> Unit = {},
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    val textNoDotZero = value.toString().removeSuffix(".0").ifEmpty { "0" }

    ScrollableTextField(
        modifier = modifier,
        listState = listState,
        vertical = vertical,
        value = textNoDotZero,
        label = label,
        labelColor = labelColor,
        labelFontSize = labelFontSize,
        disableSystemKeyboard = disableSystemKeyboard,
        onFocusChanged = onFocusChanged,
        onValueChanged = { newValue ->
            validateDouble(
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

private fun validateDouble(
    newValue: String,
    existingValue: String,
    maxValue: Double,
    minValue: Double,
    precision: Int,
    onValueChanged: (Double) -> Unit,
): String {
    var newValueAsDouble = newValue.trim().toDoubleOrNull()
    var text: String = existingValue

    if (newValueAsDouble != null) {
        if (newValueAsDouble <= maxValue && newValue.substringAfter('.', "").length <= precision) {
            newValueAsDouble = if (newValueAsDouble >= minValue) newValueAsDouble else minValue
            text = newValueAsDouble.toString()
            onValueChanged(newValueAsDouble)
        }
    } else if (
        newValue.isEmpty() ||
        (newValue.last() == '.' && newValue.count { it == '.' } == 1)
    ) {
        text = newValue
    }

    return text
}
