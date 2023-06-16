package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.browntowndev.liftlab.ui.views.utils.IconDropdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun LiftDropdown(
    hasCustomLiftSets: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceMovementPattern: () -> Unit,
    onReplaceLift: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var dropdownExpanded by remember { mutableStateOf(false) }
    var customLiftsEnabled by remember { mutableStateOf(hasCustomLiftSets) }

    IconDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = dropdownExpanded,
        onToggleExpansion = { dropdownExpanded = !dropdownExpanded }
    ) {
        DropdownMenuItem(
            text = { Text("Replace Movement Pattern") },
            onClick = {
                dropdownExpanded = false
                onReplaceMovementPattern.invoke()
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Replace Lift") },
            onClick = {
                dropdownExpanded = false
                onReplaceLift.invoke()
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Custom Sets") },
            onClick = {
                customLiftsEnabled = !customLiftsEnabled
                coroutineScope.launch{
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
                        coroutineScope.launch{
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