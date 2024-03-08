package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    hideCursor: Boolean = false,
    placeholder: String = "",
    label: String = "",
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    labelFontSize: TextUnit = 10.sp,
    disableSystemKeyboard: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onLeftFocusBlank: () -> Unit = {},
    onValueChanged: (Float?) -> Unit = {},
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    var text by remember(value) { mutableStateOf(value?.toString()?.removeSuffix(".0") ?: "") }

    ScrollableTextField(
        modifier = modifier,
        listState = listState,
        vertical = vertical,
        errorOnEmptyString = errorOnEmpty,
        fontSize = fontSize,
        value = text,
        hideCursor = hideCursor,
        placeholder = placeholder,
        label = label,
        labelColor = labelColor,
        labelFontSize = labelFontSize,
        disableSystemKeyboard = disableSystemKeyboard,
        onFocusChanged = onFocusChanged,
        onLeftFocusBlank = onLeftFocusBlank,
        onValueChanged = { newValue ->
            text = validateFloat(
                newValue = newValue,
                existingValue = text,
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
    onValueChanged: (Float?) -> Unit,
): String {
    val text: String
    val newValueAsNumber = newValue.trim().toFloatOrNull()

    if (newValueAsNumber != null &&
        newValue.substringAfter('.', "").length <= precision
    ) {
        val minMaxEvaluatedNumber = if (newValueAsNumber in minValue..maxValue) {
            newValueAsNumber
        } else if (newValueAsNumber < minValue) {
            minValue
        } else {
            maxValue
        }

        text = if (newValue.endsWith('.') && (newValueAsNumber == minMaxEvaluatedNumber)) {
            newValue
        } else {
            minMaxEvaluatedNumber.toString().removeSuffix(".0")
        }

        onValueChanged(minMaxEvaluatedNumber)
    } else if (newValue.isEmpty()) {
        text = newValue
        onValueChanged(null)
    } else {
        text = existingValue
    }

    return text
}
