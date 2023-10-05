package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.persistence.dtos.OneRepMaxResultDto

@Composable
fun HistoryTab(
    oneRepMax: Pair<String, Float>?,
    maxVolume: Pair<String, Float>?,
    maxWeight: Pair<String, Float>?,
    topTenPerformances: List<OneRepMaxResultDto>,
    totalReps: Int,
    totalVolume: Float,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 20.dp, end = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Personal Records",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
        )
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = "Estimated 1RM",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = oneRepMax?.first ?: "N/A",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = oneRepMax?.second?.toString() ?: "N/A",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 15.sp,
            )
        }
    }
}