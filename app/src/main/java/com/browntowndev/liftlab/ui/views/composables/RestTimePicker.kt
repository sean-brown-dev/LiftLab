package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration

@Composable
fun RestTimePicker(
    restTime: Duration,
    enable: Boolean,
    onHide: () -> Unit,
    onChangeRestTime: (newRestTime: Duration, enabled: Boolean) -> Unit,
) {
    Column {
        DurationPickerMenuItem(
            enabled = enable,
            time = restTime,
            onRestTimeChanged = { newRestTime -> onChangeRestTime(newRestTime, enable) },
            onCancel = onHide,
        ) {
            HeaderSwitcher(
                label = "Enable",
                checked = enable,
                onChanged = {
                    onChangeRestTime(restTime, it)
                }
            )
        }
    }
}

@Composable
fun HeaderSwitcher(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.weight(.1f))
        var checkedState by remember(checked) { mutableStateOf(checked) }
        Switch(
            modifier = Modifier.padding(end = 5.dp),
            enabled = true,
            checked = checkedState,
            onCheckedChange = {
                checkedState = it
                onChanged(it)
            },
            colors = SwitchDefaults.colors(
                checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedIconColor = MaterialTheme.colorScheme.onTertiary,
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            )
        )
    }
}