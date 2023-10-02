package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

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
import com.browntowndev.liftlab.ui.views.composables.NumberPickerMenuItem

@Composable
fun IncrementPicker(
    increment: Float,
    applyAcrossWorkouts: Boolean,
    onHide: () -> Unit,
    onChangeIncrement: (newIncrement: Float, applyAcrossWorkouts: Boolean) -> Unit,
) {
    Column {
        var applyAcrossWorkoutsState by remember(applyAcrossWorkouts) { mutableStateOf(applyAcrossWorkouts) }
        NumberPickerMenuItem(
            initialValue = increment,
            label = "Weight Increment",
            options = listOf(.5f, 1f, 2.5f, 5f, 10f),
            onConfirm = {
                onChangeIncrement(it, applyAcrossWorkoutsState)
                onHide()
            },
            onCancel = onHide,
        ) {
            Row(
                modifier = Modifier.padding(start = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Use Across Workouts",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.weight(.1f))
                Switch(
                    modifier = Modifier.padding(end = 5.dp),
                    enabled = true,
                    checked = applyAcrossWorkoutsState,
                    onCheckedChange = {
                        applyAcrossWorkoutsState = it
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
    }
}