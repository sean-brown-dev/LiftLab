package com.browntowndev.liftlab.ui.views.main.lab

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.browntowndev.liftlab.ui.composables.IconDropdown


@Composable
fun WorkoutMenuDropdown(
    showEditWorkoutNameModal: () -> Unit,
    beginDeleteWorkout: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    IconDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = isExpanded,
        onToggleExpansion = { isExpanded = !isExpanded }
    ) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = {
                isExpanded = false
                showEditWorkoutNameModal()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            })
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                isExpanded = false
                beginDeleteWorkout()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            })
    }
}