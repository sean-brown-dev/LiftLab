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

@Composable
fun Totals(
    totalReps: String,
    totalVolume: String,
) {
    SectionLabel(text = "TOTALS")
    Column(
        modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TotalRow(label = "Total Reps", total = totalReps)
        TotalRow(label = "Total Volume", total = totalVolume)
    }
}

@Composable
private fun TotalRow(label: String, total: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = total,
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 18.sp,
        )
    }
}