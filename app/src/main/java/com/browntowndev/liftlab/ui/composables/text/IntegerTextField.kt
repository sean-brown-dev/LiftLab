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
import com.browntowndev.liftlab.core.common.toIntOrNullWithClamping
import kotlin.math.max
import kotlin.math.min

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
    emitOnlyOnLostFocus: Boolean = false,
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
    // 1) Defensive bounds: coerceIn will throw if min > max, so clamp with a safe range
    val safeMin = min(minValue, maxValue)
    val safeMax = max(minValue, maxValue)

    var isFocused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(value?.toString() ?: "") }
    // Always read the freshest committed value inside Launched Effects
    val latestValue by rememberUpdatedState(value)

    LaunchedEffect(isFocused) {
        if (!isFocused) {
            withFrameNanos { /* no-op, just wait a frame so VM can complete update */ }
            text = latestValue?.toString() ?: ""
        }
    }

    LaunchedEffect(value) {
        if (!isFocused) {
            text = latestValue?.toString() ?: ""
        }
    }

    ScrollableTextField(
        modifier = modifier,
        placeholder = placeholder,
        listState = listState,
        vertical = vertical,
        value = text,
        errorOnEmptyString = errorOnEmpty,
        label = label,
        fontSize = fontSize,
        labelColor = labelColor,
        labelFontSize = labelFontSize,
        disableSystemKeyboard = disableSystemKeyboard,
        onFocusChanged = { focused ->
            onFocusChanged(focused)

            val newValue = text.toIntOrNullWithClamping(
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
        onValueChanged = { raw ->
            val newValue = raw.toIntOrNullWithClamping(
                minValue = safeMin,
                maxValue = safeMax,
            )

            when {
                !emitOnlyOnLostFocus && newValue == null -> onValueChanged(newValue)
                !emitOnlyOnLostFocus && newValue != null -> {
                    onValueChanged(newValue)
                    onNonNullValueChanged(newValue)
                }
            }

            text = newValue?.toString() ?: raw
        },
        onPixelOverflowChanged = onPixelOverflowChanged,
    )
}
