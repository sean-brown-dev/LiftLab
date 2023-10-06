package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.OneRepMaxEntry

@Composable
fun HistoryTab(
    oneRepMax: Pair<String, String>?,
    maxVolume: Pair<String, String>?,
    maxWeight: Pair<String, String>?,
    totalReps: String,
    totalVolume: String,
    topTenPerformances: List<OneRepMaxEntry>,
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
        PersonalRecords(
            oneRepMax = oneRepMax,
            maxVolume = maxVolume,
            maxWeight = maxWeight,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Totals(totalReps = totalReps, totalVolume = totalVolume)
        Spacer(modifier = Modifier.height(30.dp))
        TopTenPerformances(topTenPerformances = topTenPerformances)
    }
}