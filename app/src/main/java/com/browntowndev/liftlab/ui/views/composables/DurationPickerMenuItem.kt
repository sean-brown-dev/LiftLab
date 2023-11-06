package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration


@Composable
fun DurationPickerMenuItem(
    enabled: Boolean,
    time: Duration,
    rangeInMinutes: LongRange = 0L..6L,
    secondsStepSize: Int = 5,
    onRestTimeChanged: (Duration) -> Unit,
    onCancel: () -> Unit,
    headerContent: @Composable (ColumnScope.() -> Unit)
) {
    Column(modifier = Modifier.width(200.dp)) {
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = "Rest Time",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 10.sp,
            )
        }
        headerContent()
        if (enabled) {
            TimeSelectionSpinner(
                time = time,
                rangeInMinutes = rangeInMinutes,
                secondsStepSize = secondsStepSize,
                onTimeChanged = onRestTimeChanged
            )
        }
    }
}
