package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
        item {
            PersonalRecords(
                oneRepMax = oneRepMax,
                maxVolume = maxVolume,
                maxWeight = maxWeight,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, bottom = 25.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(.95f),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Totals(totalReps = totalReps, totalVolume = totalVolume)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, bottom = 25.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(.95f),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        topTenPerformances(lazyListScope = this, topTenPerformances = topTenPerformances)
    }
}