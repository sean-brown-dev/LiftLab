package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.views.composables.SectionLabel


@Composable
fun PersonalRecords(
    oneRepMax: Pair<String, String>?,
    maxVolume: Pair<String, String>?,
    maxWeight: Pair<String, String>?,
) {
    SectionLabel(
        modifier = Modifier.padding(top = 20.dp),
        text = "PERSONAL RECORDS"
    )
    Column(
        modifier = Modifier.padding(start = 10.dp, end = 20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PersonalRecordRow(
            prLabel = "Estimated 1RM",
            date = oneRepMax?.first ?: "N/A",
            recordValue = oneRepMax?.second ?: "N/A",
        )
        PersonalRecordRow(
            prLabel = "Max Volume",
            date = maxVolume?.first ?: "N/A",
            recordValue = maxVolume?.second ?: "N/A",
        )
        PersonalRecordRow(
            prLabel = "Max Weight",
            date = maxWeight?.first ?: "N/A",
            recordValue = maxWeight?.second ?: "N/A",
        )
    }
}

@Composable
fun PersonalRecordRow(
    prLabel: String,
    date: String?,
    recordValue: String?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = prLabel,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = recordValue ?: "N/A",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 18.sp,
            )
            Text(
                text = date ?: "N/A",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 12.sp,
            )
        }
    }
}