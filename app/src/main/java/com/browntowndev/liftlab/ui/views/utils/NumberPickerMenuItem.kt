package com.browntowndev.liftlab.ui.views.utils

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun NumberPickerMenuItem(
    initialValue: Float,
    label: String,
    options: List<Float>,
    onConfirm: (Float) -> Unit,
    onCancel: () -> Unit,
    headerContent: @Composable (() -> Unit) = {},
) {
    var optionValues by remember { mutableStateOf(options) }
    var displayValues by remember {
        mutableStateOf(options.fastMap {
            if (it % 1 == 0f) {
                String.format("%.0f", it) // Format as whole number if no decimal places
            } else {
                String.format("%.1f", it) // Format with 1 decimal place if there are non-zero decimals
            }
        }.toTypedArray())
    }
    var selectedValue by remember { mutableStateOf(initialValue) }

    LaunchedEffect(initialValue) {
        selectedValue = initialValue
    }
    LaunchedEffect(options) {
        optionValues = options
        displayValues = options.fastMap {
            if (it % 1 == 0f) {
                String.format("%.0f", it) // Format as whole number if no decimal places
            } else {
                String.format("%.1f", it) // Format with 1 decimal place if there are non-zero decimals
            }
        }.toTypedArray()
    }

    Column(modifier = Modifier.width(200.dp)) {
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 10.sp,
            )
            IconButton(onClick = { onConfirm(selectedValue)}) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
        }
        headerContent()
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            var selectedIndex by remember { mutableStateOf(optionValues.indexOf(selectedValue)) }
            LaunchedEffect(key1 = selectedValue, key2 = optionValues) {
                selectedIndex = optionValues.indexOf(selectedValue)
            }
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    NumberPicker(context).apply {
                        setOnValueChangedListener { _, _, newVal ->
                            selectedIndex = newVal
                            selectedValue = optionValues[newVal]
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
}