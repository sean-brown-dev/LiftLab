package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.ui.composables.IconDropdown
import com.browntowndev.liftlab.ui.composables.RestTimePicker
import kotlin.time.Duration

@Composable
fun LiftDropdown(
    restTime: Duration,
    restTimerEnabled: Boolean,
    onChangeRestTime: (newRestTime: Duration, enabled: Boolean) -> Unit,
    onReplaceLift: () -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    IconDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = dropdownExpanded,
        onToggleExpansion = { dropdownExpanded = !dropdownExpanded }
    ) {
        var showRestTimePicker by remember { mutableStateOf(false) }

        if (showRestTimePicker) {
            RestTimePicker(
                restTime = restTime,
                enable = restTimerEnabled,
                onHide = { showRestTimePicker = !showRestTimePicker },
                onChangeRestTime = onChangeRestTime,
            )
        } else {
            DropdownMenuItem(
                text = { Text("Replace") },
                onClick = {
                    dropdownExpanded = false
                    onReplaceLift()
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.replace_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Rest Time") },
                onClick = {
                    showRestTimePicker = true
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.stopwatch_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                },
                trailingIcon = {
                    val restTimeDisplay by remember(key1 = restTime, key2 = restTimerEnabled) {
                        mutableStateOf(
                            if (restTimerEnabled) {
                                restTime.toTimeString()
                            } else "Off"
                        )
                    }

                    Text(text = restTimeDisplay, color = MaterialTheme.colorScheme.tertiary)
                }
            )
        }
    }
}