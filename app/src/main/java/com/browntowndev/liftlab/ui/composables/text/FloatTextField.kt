package com.browntowndev.liftlab.ui.composables.text

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.toFloatOrNullWithRoundingAndClamping
import kotlin.math.max
import kotlin.math.min

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
    emitOnlyOnLostFocus: Boolean = false,
    updateValueWhileFocused: Boolean = false,
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
    onNonNullValueChanged: (Float) -> Unit = {},
    onPixelOverflowChanged: (Dp) -> Unit= {},
) {
    // 1) Defensive bounds: coerceIn will throw if min > max, so clamp with a safe range
    val safeMin = min(minValue, maxValue)
    val safeMax = max(minValue, maxValue)

    var isFocused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(value?.toString()?.removeSuffix(".0") ?: "") }
    // Always read the freshest committed value inside Launched Effects
    val latestValue by rememberUpdatedState(value)

    LaunchedEffect(isFocused) {
        if (!isFocused) {
            withFrameNanos { /* no-op, just wait a frame so VM can complete update */ }
            text = latestValue?.toString()?.removeSuffix(".0") ?: ""
        }
    }

    LaunchedEffect(value) {
        if (!isFocused || updateValueWhileFocused) {
            text = latestValue?.toString()?.removeSuffix(".0") ?: ""
        }
    }

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
        onFocusChanged = { focused ->
            onFocusChanged(focused)

            val newValue = text.toFloatOrNullWithRoundingAndClamping(
                precision = precision,
                minValue = safeMin,
                maxValue = safeMax,
            )

            val shouldEmit = emitOnlyOnLostFocus && !focused
            when {
                shouldEmit && newValue == null -> onValueChanged(newValue)
                shouldEmit && newValue != null -> {
                    onValueChanged(newValue)
                    onNonNullValueChanged(newValue)
                }
            }

            isFocused = focused
        },
        onLeftFocusBlank = onLeftFocusBlank,
        onValueChanged = { newValue ->
            text = validateFloat(
                newValue = newValue,
                existingValue = text,
                maxValue = safeMax,
                minValue = safeMin,
                precision = precision,
                onValueChanged = { newValue ->
                    when {
                        !emitOnlyOnLostFocus && newValue == null -> onValueChanged(newValue)
                        !emitOnlyOnLostFocus && newValue != null -> {
                            onValueChanged(newValue)
                            onNonNullValueChanged(newValue)
                        }
                    }
                }
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
    val newValueAsNumber = newValue.toFloatOrNullWithRoundingAndClamping(
        precision = precision,
        minValue = minValue,
        maxValue = maxValue,
    )

    if (newValueAsNumber != null) {
        val newValueWithoutSuffix = newValueAsNumber.toString().removeSuffix(".0")
        text = if (newValue.endsWith('.')) {
            "$newValueWithoutSuffix."
        } else newValueWithoutSuffix

        onValueChanged(newValueAsNumber)
    } else if (newValue.isEmpty()) {
        text = newValue
        onValueChanged(null)
    } else {
        text = existingValue
    }

    return text
}
