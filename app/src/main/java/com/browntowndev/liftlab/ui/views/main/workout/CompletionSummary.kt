package com.browntowndev.liftlab.ui.views.main.workout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.toShortTimeString
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.core.common.toWholeNumberOrOneDecimalString
import com.browntowndev.liftlab.ui.models.WorkoutCompletionSummary
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CompletionSummary(
    paddingValues: PaddingValues,
    workoutCompletionSummary: WorkoutCompletionSummary,
    startTime: Date,
    onShare: (workoutSummaryImage: Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val workoutSummaryCaptureController = rememberCaptureController()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Card(
                    modifier = Modifier.capturable(workoutSummaryCaptureController),
                    shape = RectangleShape,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = workoutCompletionSummary.workoutName,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 28.sp,
                        )
                        Row (
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${workoutCompletionSummary.percentageComplete.toWholeNumberOrOneDecimalString()}% Complete",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            if (workoutCompletionSummary.totalIncompleteLifts > 0) {
                                val liftOrLifts = if (workoutCompletionSummary.totalIncompleteLifts > 1) "Lifts" else "Lift"
                                Text(
                                    modifier = Modifier.padding(start = 5.dp),
                                    text = "${workoutCompletionSummary.totalIncompleteLifts} Incomplete $liftOrLifts",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Row (
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.award_medals),
                                tint = MaterialTheme.colorScheme.tertiary,
                                contentDescription = null,
                            )
                            val recordOrRecords = if (workoutCompletionSummary.personalRecordCount == 1) {
                                "Personal Record"
                            } else {
                                "Personal Records"
                            }
                            Text(
                                text = "${workoutCompletionSummary.personalRecordCount} $recordOrRecords",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 18.sp,
                            )
                        }
                        Row (verticalAlignment = Alignment.CenterVertically) {
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
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(.8f),
                                text = "Lift",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                modifier = Modifier.weight(.5f),
                                text = "Best Set",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                modifier = Modifier.weight(.15f),
                                text = "1RM",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
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
                                    color = if (liftCompletion.isNewPersonalRecord) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        color
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
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

        val scope = rememberCoroutineScope()
        Box (
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopEnd,
        ) {
            val context = LocalContext.current
            var shareError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(key1 = shareError) {
                if (shareError?.isNotEmpty() == true) {
                    Toast.makeText(context, shareError, Toast.LENGTH_LONG).show()
                }
            }
            IconButton(
                modifier = Modifier.padding(top = 15.dp, end = 10.dp),
                onClick = {
                    scope.launch {
                        shareWorkoutSummary(
                            workoutSummaryCaptureController = workoutSummaryCaptureController,
                            context = context,
                            onShare = onShare,
                            onError = { shareError = it }
                        )
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.size(28.dp),
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.share_workout),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeApi::class)
private suspend fun shareWorkoutSummary(
    workoutSummaryCaptureController: CaptureController,
    context: Context,
    onShare: (workoutSummaryImage: Bitmap) -> Unit,
    onError: (errorMsg: String) -> Unit,
) {
    try {
        workoutSummaryCaptureController
            .captureAsync()
            .await()
            .let { imgBitmap ->
                val liftSummaryCardBitmap = imgBitmap
                    .asAndroidBitmap()
                    .copy(Bitmap.Config.ARGB_8888, true)

                // Create a new bitmap with the same dimensions as the original
                val resultBitmap = Bitmap.createBitmap(
                    liftSummaryCardBitmap.width,
                    liftSummaryCardBitmap.height,
                    Bitmap.Config.ARGB_8888,
                )

                // Switch to the Main dispatcher to perform UI operations
                withContext(Dispatchers.Main) {
                    // Create a canvas to draw on the new bitmap
                    val canvas = Canvas(resultBitmap)

                    // Draw the original bitmap on the new canvas
                    canvas.drawBitmap(liftSummaryCardBitmap, 0f, 0f, null)

                    // Draw the vector on the canvas
                    VectorDrawableCompat.create(
                        context.resources,
                        R.drawable.lift_lab_qr_code,
                        null
                    )?.let { vectorDrawable ->
                        val intrinsicWidth = vectorDrawable.intrinsicWidth
                        val intrinsicHeight = vectorDrawable.intrinsicHeight
                        val scaledHeight = 250f
                        val scaleFactor = scaledHeight / intrinsicHeight
                        val scaledWidth = (intrinsicWidth * scaleFactor).toInt()

                        vectorDrawable.setBounds(
                            liftSummaryCardBitmap.width - scaledWidth,
                            20,
                            liftSummaryCardBitmap.width - 20,
                            scaledHeight.toInt(),
                        )

                        vectorDrawable.draw(canvas)
                    }
                }

                // Now resultBitmap contains the original bitmap with the vector drawn on top
                // Share the resultBitmap instead of the original one
                onShare(resultBitmap)
            }
    } catch (error: Throwable) {
        Log.e(Log.ERROR.toString(), error.toString())
        onError("Failed to share image.")
    }
}
