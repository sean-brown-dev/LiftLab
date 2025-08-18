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
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }

    // 1) Defensive bounds: coerceIn will throw if min > max, so clamp with a safe range
    val safeMin = min(minValue, maxValue)
    val safeMax = max(minValue, maxValue)

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
        onFocusChanged = onFocusChanged,
        onLeftFocusBlank = onLeftFocusBlank,
        onValueChanged = { raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                if (!errorOnEmpty) {
                    onValueChanged(null)
                }
                text = raw
                return@ScrollableTextField
            }

            val parsed = trimmed.toIntOrNull() ?: return@ScrollableTextField
            val clamped = parsed.coerceIn(safeMin, safeMax)

            onValueChanged(clamped)
            onNonNullValueChanged(clamped)
            text = clamped.toString()
        },
        onPixelOverflowChanged = onPixelOverflowChanged,
    )
}
