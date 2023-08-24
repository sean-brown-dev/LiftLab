package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.browntowndev.liftlab.ui.views.composables.IconDropdown
import com.browntowndev.liftlab.ui.views.composables.RestTimePicker
import kotlin.time.Duration

@Composable
fun LiftDropdown(
    restTime: Duration,
    restTimeAppliedAcrossWorkouts: Boolean,
    onChangeRestTime: (newRestTime: Duration, applyToLift: Boolean) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    IconDropdown(
        iconTint = MaterialTheme.colorScheme.primary,
        isExpanded = dropdownExpanded,
        onToggleExpansion = { dropdownExpanded = !dropdownExpanded }
    ) {
        RestTimePicker(
            restTime = restTime,
            applyAcrossWorkouts = restTimeAppliedAcrossWorkouts,
            onHide = { dropdownExpanded = !dropdownExpanded },
            onChangeRestTime = onChangeRestTime,
        )
    }
}