package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach

@Composable
fun RpeKeyboard(
    modifier: Modifier = Modifier,
    visible: Boolean,
    selectedRpe: Float?,
    onRpeSelected: (rpe: Float) -> Unit,
) {
    CustomKeyboard(modifier = modifier, visible = visible) {
        var selectedRpeOption: Float? by remember(selectedRpe) { mutableStateOf(selectedRpe) }
        Box(modifier = Modifier.weight(.6f), contentAlignment = Alignment.BottomCenter) {
            if (selectedRpeOption != null) {
                val repsLeftInTank = 10.0 - selectedRpeOption!!
                val repsLeftInTankText = if (repsLeftInTank.toInt().toDouble() == repsLeftInTank) {
                    if (repsLeftInTank > 1) {
                        "You could comfortably perform ${repsLeftInTank.toInt()} more reps before failure."
                    } else if (repsLeftInTank == 1.0) {
                        "You could comfortably perform ${repsLeftInTank.toInt()} more rep before failure."
                    } else {
                        "Maximal exertion. No more reps possible."
                    }
                } else {
                    val bottomRange = repsLeftInTank.toInt().toString()
                    val topRange = (repsLeftInTank + .5).toInt().toString()
                    "You could perform $bottomRange - $topRange more reps before failure."
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Text(
                        text = repsLeftInTankText,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Text(
                        text = "Please select a Rate of Perceived Exertion (RPE) value." +
                                "This is a way of measuring the difficulty of a set.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                val rpeMinValue = remember { 6f }
                val rpeMaxValue = remember { 10f }
                val rpeOptions =
                    remember { ((rpeMinValue * 2).toInt()..(rpeMaxValue * 2).toInt() step 1).map { it / 2f } }
                rpeOptions.fastForEach { rpe ->
                    RpeOption(
                        modifier = Modifier.weight(1f),
                        isSelected = remember(selectedRpeOption) { selectedRpeOption == rpe },
                        value = rpe,
                        isFirst = remember { rpe == rpeMinValue },
                        isLast = remember { rpe == rpeMaxValue },
                        selected = {
                            selectedRpeOption = rpe
                            onRpeSelected(rpe)
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(.45f))
    }
}

@Composable
private fun RpeOption(
    modifier: Modifier = Modifier,
    value: Float,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean = false,
    selected: () -> Unit,
) {
    val rpe by remember { mutableStateOf(value.toString().removeSuffix(".0")) }
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val backgroundColor = MaterialTheme.colorScheme.background

    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable { selected() },
        color = remember (isSelected) { if (isSelected) tertiaryColor else backgroundColor },
        shape = RoundedCornerShape(
                topStart = remember { if (isFirst) 16.dp else 0.dp },
                topEnd = remember { if (isLast) 16.dp else 0.dp },
                bottomStart = remember { if (isFirst) 16.dp else 0.dp },
                bottomEnd = remember { if (isLast) 16.dp else 0.dp }),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 5.dp),
                text = rpe,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
