package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.chargemap.compose.numberpicker.NumberPicker

@Composable
fun LabeledNumberPicker(label: String, value: Int, range: Iterable<Int>, onValueChange: (Int) -> Unit) {
    var localValue by remember { mutableStateOf(value) }
    Column(horizontalAlignment = Alignment.CenterHorizontally)
    {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
        )
        NumberPicker(
            value = localValue,
            dividersColor = MaterialTheme.colorScheme.onPrimaryContainer,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.primary),
            onValueChange = {
                localValue = it
                onValueChange.invoke(it)
            },
            range = range
        )
    }
}