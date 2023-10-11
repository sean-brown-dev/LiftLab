package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toSimpleDateTimeString
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import java.util.Date
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WorkoutHistoryCard(
    workoutLogEntryId: Long,
    workoutName: String,
    workoutDate: Date,
    workoutDuration: Long,
    setResults: List<SetLogEntryDto>,
    topSets: Map<String, Pair<Int, SetLogEntryDto>>,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 5.dp, horizontal = 10.dp),
        shape = CardDefaults.shape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        onClick = { /*TODO*/ }
    ) {
        val totalPersonalRecords = remember(topSets) { setResults.count { it.isPersonalRecord } }
        Text(
            text = workoutName,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 15.dp, top = 10.dp)
        )
        Text(
            modifier = Modifier.padding(start = 15.dp, bottom = 10.dp),
            text = workoutDate.toSimpleDateTimeString(),
            color = MaterialTheme.colorScheme.outline,
            fontSize = 15.sp,
        )
        Row(
            modifier = Modifier.padding(start = 15.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                modifier = Modifier.size(15.dp),
                painter = painterResource(id = R.drawable.stopwatch_icon),
                tint = MaterialTheme.colorScheme.outline,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(end = 10.dp),
                text = workoutDuration.toTimeString(),
                color = MaterialTheme.colorScheme.outline,
                fontSize = 15.sp,
            )
            Icon(
                modifier = Modifier.size(15.dp),
                painter = painterResource(id = R.drawable.square_weight_icon),
                tint = MaterialTheme.colorScheme.tertiary,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(end = 10.dp),
                text = "$totalPersonalRecords PRs",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 15.sp,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.padding(horizontal = 15.dp),
                text = "Lift",
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.padding(horizontal = 15.dp),
                text = "Best Set",
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        val liftIds = remember(setResults) { setResults.distinctBy { it.liftId }.map { it.liftId } }
        liftIds.fastForEach { liftId ->
            val topSet = remember(topSets) { topSets["${workoutLogEntryId}-$liftId"] }
            if (topSet != null) {
                val weight = remember(topSet) {
                    if (topSet.second.weight.isWholeNumber()) {
                        topSet.second.weight.roundToInt().toString()
                    }
                    else {
                        String.format("%.2f", topSet.second.weight)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${topSet.first} x ${topSet.second.liftName}",
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 15.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$weight x ${topSet.second.reps} @${topSet.second.rpe}",
                        color = if (topSet.second.isPersonalRecord) {
                            MaterialTheme.colorScheme.tertiary
                        }
                        else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 15.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(15.dp))
    }
}