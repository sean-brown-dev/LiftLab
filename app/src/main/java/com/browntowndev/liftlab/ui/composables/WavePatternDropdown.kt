package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme

@Composable
fun WavePatternDropdown(
    progressionScheme: ProgressionScheme,
    workoutLiftSetSteps: Map<Long, List<Int>>,
    workoutLiftId: Long,
    workoutLiftStepSizeOptions: Map<Long, List<Pair<Int, List<Int>>>>,
    onUpdateStepSize: (workoutLiftId: Long, stepSize: Int) -> Unit,
) {
    val stepSizeOptions = remember(workoutLiftStepSizeOptions) {
        workoutLiftStepSizeOptions[workoutLiftId] ?: listOf()
    }

    if (stepSizeOptions.isNotEmpty()) {
        Row(modifier = Modifier.padding(top = 5.dp, start = 20.dp)) {
            var isExpanded by remember { mutableStateOf(false) }
            val stepsToBeTaken = remember(workoutLiftSetSteps) {
                workoutLiftSetSteps[workoutLiftId] ?: listOf()
            }
            Text(
                modifier = Modifier.padding(end = 5.dp),
                text = "Wave Pattern:",
                fontSize = 16.sp,
            )
            CustomAnchorDropdown(
                isExpanded = isExpanded,
                onToggleExpansion = { isExpanded = !isExpanded },
                anchor = { modifier ->
                    Box(modifier = modifier, contentAlignment = Alignment.Center) {
                        WavePattern(setCounts = stepsToBeTaken)
                    }
                }
            ) {
                stepSizeOptions.fastForEach { option ->
                    DropdownMenuItem(
                        text = { WavePattern(setCounts = option.second, fontColor = MaterialTheme.colorScheme.onBackground) },
                        onClick = {
                            onUpdateStepSize(workoutLiftId, option.first)
                            isExpanded = false
                        })
                }
            }
        }
    } else if (progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION) {
        Row (
            modifier = Modifier.padding(top = 5.dp, start = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(18.dp).padding(end = 5.dp),
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Uneven wave pattern. Adjust rep ranges or deload week.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun WavePattern(
    setCounts: List<Int>,
    fontColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        setCounts.fastForEachIndexed { i, step ->
            Text(
                text = step.toString(),
                fontSize = 18.sp,
                color = fontColor,
            )
            if (i < setCounts.size - 1) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = fontColor,
                )
            }
        }
    }
}