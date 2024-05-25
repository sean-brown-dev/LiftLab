package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.toShortTimeString
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.core.common.toWholeNumberOrOneDecimalString
import com.browntowndev.liftlab.ui.models.WorkoutCompletionSummary
import java.util.Date

@Composable
fun CompletionSummary(
    paddingValues: PaddingValues,
    workoutCompletionSummary: WorkoutCompletionSummary,
    startTime: Date,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Text(
            modifier = Modifier.padding(start = 5.dp, top = 10.dp),
            text = workoutCompletionSummary.workoutName,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
        )
        Row (
            modifier = Modifier.padding(start = 5.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${workoutCompletionSummary.percentageComplete.toWholeNumberOrOneDecimalString()}% Complete",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            if (workoutCompletionSummary.totalIncompleteLifts > 0) {
                val liftOrLifts = if (workoutCompletionSummary.totalIncompleteLifts > 1) "Lifts" else "Lift"
                Text(
                    modifier = Modifier.padding(start = 5.dp),
                    text = "${workoutCompletionSummary.totalIncompleteLifts} Incomplete $liftOrLifts",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row (
            modifier = Modifier.padding(start = 2.dp, top = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Filled.PlayArrow,
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = "Started",
            )
            Text(
                modifier = Modifier.padding(start = 2.dp, end = 15.dp),
                text = startTime.toShortTimeString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 14.sp,
            )
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = R.drawable.stop_circle),
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = "Stopped",
            )
            Text(
                modifier = Modifier.padding(start = 2.dp, end = 15.dp),
                text = workoutCompletionSummary.endTime.toShortTimeString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 14.sp,
            )
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = R.drawable.timelapse),
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = "Duration",
            )
            Text(
                modifier = Modifier.padding(start = 2.dp, end = 15.dp),
                text = remember(startTime, workoutCompletionSummary.endTime) {
                    (workoutCompletionSummary.endTime.time - startTime.time).toTimeString()
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 14.sp,
            )
        }
        Card(
            modifier = Modifier.padding(top = 30.dp),
            shape = RectangleShape,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(.8f),
                        text = "Lift",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        modifier = Modifier.weight(.5f),
                        text = "Best Set",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        modifier = Modifier.weight(.15f),
                        text = "1RM",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                workoutCompletionSummary.liftCompletionSummaries.fastForEach { liftCompletion ->
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val color = if (liftCompletion.isIncomplete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        Text(
                            modifier = Modifier.weight(.8f),
                            text = "${liftCompletion.setsCompleted} x ${liftCompletion.liftName}",
                            fontSize = 14.sp,
                            color = color,
                        )
                        Text(
                            modifier = Modifier.weight(.5f),
                            text = "${liftCompletion.bestSetReps}x${liftCompletion.bestSetWeight} @${liftCompletion.bestSetRpe}",
                            fontSize = 14.sp,
                            color = color,
                        )
                        Text(
                            modifier = Modifier.weight(.15f),
                            text = "${liftCompletion.bestSet1RM}",
                            fontSize = 14.sp,
                            color = color,
                        )
                    }
                }
            }
        }
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                modifier = Modifier.padding(end = 10.dp),
                onClick = onCancel,
            ) {
                Text(
                    text = "Continue Workout",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}