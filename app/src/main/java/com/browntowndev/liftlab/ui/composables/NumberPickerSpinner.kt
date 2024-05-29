package com.browntowndev.liftlab.ui.composables

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.AndroidView
import com.browntowndev.liftlab.core.common.toWholeNumberOrOneDecimalString

@Composable
fun NumberPickerSpinner(
    modifier: Modifier = Modifier,
    options: List<Float>,
    initialValue: Float,
    onChanged: (Float) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val optionValues by remember(options) { mutableStateOf(options) }
        val displayValues by remember(options) {
            mutableStateOf(options.fastMap {
                it.toWholeNumberOrOneDecimalString()
            }.toTypedArray())
        }
        var selectedValue by remember(initialValue) { mutableFloatStateOf(initialValue) }
        var selectedIndex by remember(
            selectedValue,
            optionValues
        ) { mutableIntStateOf(optionValues.indexOf(selectedValue)) }
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                NumberPicker(context).apply {
                    setOnValueChangedListener { _, _, newVal ->
                        selectedIndex = newVal
                        selectedValue = optionValues[newVal]
                        onChanged(selectedValue)
                    }
                    displayedValues = displayValues
                    minValue = 0
                    maxValue = optionValues.size - 1
                    value = selectedIndex
                    wrapSelectorWheel = false
                }
            },
        )
    }
}

@Composable
fun NullableNumberPickerSpinner(
    modifier: Modifier = Modifier,
    options: List<Float?>,
    initialValue: Float?,
    onChanged: (Float?) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val optionValues by remember(options) { mutableStateOf(options) }
        val displayValues by remember(options) {
            mutableStateOf(options.fastMap {
                it?.toWholeNumberOrOneDecimalString() ?: "None"
            }.toTypedArray())
        }
        var selectedValue by remember(initialValue) { mutableStateOf(initialValue) }
        var selectedIndex by remember(
            selectedValue,
            optionValues
        ) { mutableIntStateOf(optionValues.indexOf(selectedValue)) }
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                NumberPicker(context).apply {
                    setOnValueChangedListener { _, _, newVal ->
                        selectedIndex = newVal
                        selectedValue = optionValues[newVal]
                        onChanged(selectedValue)
                    }
                    displayedValues = displayValues
                    minValue = 0
                    maxValue = optionValues.size - 1
                    value = selectedIndex
                    wrapSelectorWheel = false
                }
            },
        )
    }
}
