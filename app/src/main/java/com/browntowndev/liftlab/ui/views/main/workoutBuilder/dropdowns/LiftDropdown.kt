package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.views.utils.IconDropdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun LiftDropdown(
    hasCustomLiftSets: Boolean,
    showCustomSetsOption: Boolean,
    currentDeloadWeek: Int?,
    showDeloadWeekOption: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceLift: () -> Unit,
    onDeleteLift: () -> Unit,
    onChangeDeloadWeek: () -> Unit,
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
        if (showCustomSetsOption || showDeloadWeekOption) {
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        }
        if (showDeloadWeekOption) {
            DropdownMenuItem(
                text = { Text("Deload Week") },
                onClick = {
                    dropdownExpanded = false
                    onChangeDeloadWeek()
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
                    if(currentDeloadWeek != null) {
                        Text(currentDeloadWeek.toString(), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            )
        }
        if (showCustomSetsOption) {
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
}