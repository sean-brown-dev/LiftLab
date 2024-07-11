package com.browntowndev.liftlab.ui.composables

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun TimeSelectionSpinner(
    modifier: Modifier = Modifier,
    time: Duration,
    rangeInMinutes: LongRange,
    secondsStepSize: Int,
    onTimeChanged: (Duration) -> Unit,
) {
    var seconds by remember(Unit) { mutableLongStateOf(time.inWholeSeconds % 60) }
    var minutes by remember(time) {
        mutableLongStateOf(
            if (time.inWholeMinutes > rangeInMinutes.last) rangeInMinutes.last
            else if (time.inWholeMinutes < rangeInMinutes.first) rangeInMinutes.first
            else time.inWholeMinutes
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                NumberPicker(context).apply {
                    setOnValueChangedListener { picker, _, newVal ->
                        minutes = newVal.toLong()
                        if (minutes == picker.minValue.toLong() && seconds == 0L) {
                            seconds = 5L
                        }

                        val secondsDuration = seconds.toDuration(DurationUnit.SECONDS)
                        val minutesDuration = minutes.toDuration(DurationUnit.MINUTES)
                        onTimeChanged(minutesDuration + secondsDuration)
                    }

                    setPadding(10, 0, 10, 0)
                    minValue = rangeInMinutes.first.toInt()
                    maxValue = rangeInMinutes.last.toInt()
                    value = minutes.toInt()
                    wrapSelectorWheel = true
                }
            },
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(":", fontSize = 40.sp)
        Spacer(modifier = Modifier.width(5.dp))
        if (minutes > rangeInMinutes.first) {
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    NumberPicker(context).apply {
                        setOnValueChangedListener { _, _, newVal ->
                            seconds = (newVal * secondsStepSize).toLong()

                            val secondsDuration = seconds.toDuration(DurationUnit.SECONDS)
                            val minutesDuration = minutes.toDuration(DurationUnit.MINUTES)
                            onTimeChanged(minutesDuration + secondsDuration)
                        }
                        val displayedValues = Array(12) { index ->
                            String.format(Locale.US, "%02d", index * secondsStepSize)
                        }
                        setDisplayedValues(displayedValues)
                        minValue = 0
                        maxValue = 11
                        value = (seconds / secondsStepSize).toInt()
                        wrapSelectorWheel = true
                    }
                },
            )
        } else {
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    NumberPicker(context).apply {
                        setOnValueChangedListener { _, _, newVal ->
                            seconds = (newVal * secondsStepSize).toLong()

                            val secondsDuration = seconds.toDuration(DurationUnit.SECONDS)
                            val minutesDuration = minutes.toDuration(DurationUnit.MINUTES)
                            onTimeChanged(minutesDuration + secondsDuration)
                        }
                        val displayedValues = Array(11) { index ->
                            String.format(Locale.US, "%02d", (index + 1) * secondsStepSize)
                        }
                        setDisplayedValues(displayedValues)
                        minValue = 1
                        maxValue = 11
                        value = (seconds / secondsStepSize).toInt()
                        wrapSelectorWheel = true
                    }
                },
            )
        }
    }
}