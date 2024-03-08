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
import androidx.compose.ui.util.fastForEachIndexed
import com.browntowndev.liftlab.ui.models.OneRepMaxEntry
import com.browntowndev.liftlab.ui.composables.SectionLabel

@Composable
fun TopTenPerformances(topTenPerformances: List<OneRepMaxEntry>) {
    SectionLabel(text = "TOP TEN ESTIMATED 1RM PERFORMANCES")
    Column (
        modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        topTenPerformances.fastForEachIndexed { index, performance ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (index + 1).toString(),
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Column (horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = performance.setsAndRepsLabel,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = performance.date,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = performance.oneRepMax,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 18.sp
                )
            }
        }
    }
}