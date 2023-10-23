package com.browntowndev.liftlab.ui.views.composables

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Composable
fun DurationPickerMenuItem(
    enabled: Boolean,
    startTime: Duration,
    rangeInMinutes: LongRange = 0L..6L,
    secondsStepSize: Int = 5,
    onRestTimeChanged: (Duration) -> Unit,
    onCancel: () -> Unit,
    headerContent: @Composable (ColumnScope.() -> Unit)
) {
    var seconds by remember(startTime) { mutableLongStateOf(startTime.inWholeSeconds % 60) }
    var minutes by remember(startTime) {
        mutableLongStateOf(
            if (startTime.inWholeMinutes > rangeInMinutes.last) rangeInMinutes.last
            else if (startTime.inWholeMinutes < rangeInMinutes.first) rangeInMinutes.first
            else startTime.inWholeMinutes
        )
    }

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
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            setOnValueChangedListener { _, _, newVal ->
                                minutes = newVal.toLong()

                                val secondsDuration = seconds.toDuration(DurationUnit.SECONDS)
                                val minutesDuration = minutes.toDuration(DurationUnit.MINUTES)
                                onRestTimeChanged(minutesDuration + secondsDuration)
                            }

                            setPadding(10, 0, 10, 0)
                            minValue = rangeInMinutes.first.toInt()
                            maxValue = rangeInMinutes.last.toInt()
                            value = minutes.toInt()
                            wrapSelectorWheel = false
                        }
                    },
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(":", fontSize = 40.sp)
                Spacer(modifier = Modifier.width(5.dp))
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            setOnValueChangedListener { _, _, newVal ->
                                seconds = (newVal * secondsStepSize).toLong()

                                val secondsDuration = seconds.toDuration(DurationUnit.SECONDS)
                                val minutesDuration = minutes.toDuration(DurationUnit.MINUTES)
                                onRestTimeChanged(minutesDuration + secondsDuration)
                            }
                            val displayedValues = Array(12) { index ->
                                String.format("%02d", index * secondsStepSize)
                            }
                            setDisplayedValues(displayedValues)
                            minValue = 0
                            maxValue = 11
                            value = (seconds / secondsStepSize).toInt()
                            wrapSelectorWheel = false
                        }
                    },
                )
            }
        }
    }
}