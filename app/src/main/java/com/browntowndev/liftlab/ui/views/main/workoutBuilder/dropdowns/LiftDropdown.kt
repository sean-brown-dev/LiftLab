package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.views.composables.IconDropdown
import com.browntowndev.liftlab.ui.views.composables.NumberPickerMenuItem
import com.browntowndev.liftlab.ui.views.composables.RestTimePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration


@Composable
fun LiftDropdown(
    hasCustomLiftSets: Boolean,
    showCustomSetsOption: Boolean,
    currentDeloadWeek: Int?,
    showDeloadWeekOption: Boolean,
    restTime: Duration,
    restTimerEnabled: Boolean,
    increment: Float,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceLift: () -> Unit,
    onDeleteLift: () -> Unit,
    onChangeDeloadWeek: (Int) -> Unit,
    onChangeRestTime: (newRestTime: Duration, enabled: Boolean) -> Unit,
    onChangeIncrement: (newIncrement: Float) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var dropdownExpanded by remember { mutableStateOf(false) }
    var customLiftsEnabled by remember { mutableStateOf(hasCustomLiftSets) }

    IconDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = dropdownExpanded,
        onToggleExpansion = { dropdownExpanded = !dropdownExpanded }
    ) {
        var showRestTimePicker by remember { mutableStateOf(false) }
        var showIncrementPicker by remember { mutableStateOf(false) }
        var showDeloadWeekPicker by remember { mutableStateOf(false) }

        if (showRestTimePicker) {
            RestTimePicker(
                restTime = restTime,
                enable = restTimerEnabled,
                onHide = { showRestTimePicker = false },
                onChangeRestTime = onChangeRestTime,
            )
        } else if(showIncrementPicker) {
            IncrementPicker(
                increment = increment,
                onBackPressed = { showIncrementPicker = false },
                onChangeIncrement = onChangeIncrement,
            )
        } else if (showDeloadWeekPicker) {
            NumberPickerMenuItem(
                initialValue = currentDeloadWeek!!.toFloat(),
                label = "Deload Week",
                options = listOf(3f, 4f, 5f, 6f, 7f, 8f),
                onChanged = {
                    onChangeDeloadWeek(it.toInt())
                },
                onBackPressed = { showDeloadWeekPicker = false },
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
                text = { Text("Remove") },
                onClick = {
                    dropdownExpanded = false
                    onDeleteLift()
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            if (showDeloadWeekOption) {
                DropdownMenuItem(
                    text = { Text("Deload Week") },
                    onClick = {
                        showDeloadWeekPicker = true
                    },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    trailingIcon = {
                        if (currentDeloadWeek != null) {
                            Text(
                                currentDeloadWeek.toString(),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                )
            }
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
                                "${restTime.inWholeMinutes}:${
                                    String.format(
                                        "%02d",
                                        restTime.inWholeSeconds % 60
                                    )
                                }"
                            } else "Off"
                        )
                    }

                    Text(text = restTimeDisplay, color = MaterialTheme.colorScheme.tertiary)
                }
            )
            DropdownMenuItem(
                text = { Text("Weight Increment") },
                onClick = {
                    showIncrementPicker = true
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.weight_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                },
                trailingIcon = {
                    var incrementDisplay by remember {
                        mutableStateOf(
                            increment.toString().removeSuffix(".0")
                        )
                    }
                    LaunchedEffect(increment) {
                        incrementDisplay = increment.toString().removeSuffix(".0")
                    }
                    Text(text = incrementDisplay, color = MaterialTheme.colorScheme.tertiary)
                }
            )
            if (showCustomSetsOption) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                DropdownMenuItem(
                    text = { Text("Custom Sets") },
                    onClick = {
                        customLiftsEnabled = !customLiftsEnabled
                        coroutineScope.launch {
                            delay(100)
                            dropdownExpanded = false
                            onCustomLiftSetsToggled(customLiftsEnabled)
                        }
                    },
                    leadingIcon = {
                        Switch(
                            enabled = true,
                            checked = customLiftsEnabled,
                            onCheckedChange = {
                                customLiftsEnabled = it
                                coroutineScope.launch {
                                    delay(100)
                                    dropdownExpanded = false
                                    onCustomLiftSetsToggled(it)
                                }
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
                )
            }
        }
    }
}