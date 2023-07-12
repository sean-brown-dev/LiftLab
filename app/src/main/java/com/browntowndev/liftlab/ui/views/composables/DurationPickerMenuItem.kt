package com.browntowndev.liftlab.ui.views.composables

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    startTime: Duration,
    rangeInMinutes: LongRange = 0L..6L,
    secondsStepSize: Int = 5,
    onConfirm: (Duration) -> Unit,
    onCancel: () -> Unit,
    headerContent: @Composable (() -> Unit)
) {
    var stepSize by remember { mutableStateOf(secondsStepSize) }
    var seconds by remember { mutableStateOf(startTime.inWholeSeconds % 60) }
    var minutes by remember {
        mutableStateOf(
            if (startTime.inWholeMinutes > rangeInMinutes.last) rangeInMinutes.last
            else if (startTime.inWholeMinutes < rangeInMinutes.first) rangeInMinutes.first
            else startTime.inWholeMinutes
        )
    }

    LaunchedEffect(secondsStepSize) {
        stepSize = secondsStepSize
    }

    LaunchedEffect(startTime) {
        seconds = startTime.inWholeSeconds % 60
        minutes =
            if (startTime.inWholeMinutes > rangeInMinutes.last) rangeInMinutes.last
            else if (startTime.inWholeMinutes < rangeInMinutes.first) rangeInMinutes.first
            else startTime.inWholeMinutes
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
            IconButton(onClick = {
                val secondsDuration = seconds.toDuration(DurationUnit.SECONDS)
                val minutesDuration = minutes.toDuration(DurationUnit.MINUTES)
                onConfirm(minutesDuration + secondsDuration)
            }) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
        }
        headerContent()
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    NumberPicker(context).apply {
                        setOnValueChangedListener { _, _, newVal -> minutes = newVal.toLong() }
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
                            seconds = (newVal * stepSize).toLong()
                        }
                        val displayedValues = Array(13) { index ->
                            String.format("%02d", index * stepSize)
                        }
                        setDisplayedValues(displayedValues)
                        minValue = 0
                        maxValue = 12
                        value = (seconds / stepSize).toInt()
                        wrapSelectorWheel = false
                    }
                },
            )
        }
    }
}