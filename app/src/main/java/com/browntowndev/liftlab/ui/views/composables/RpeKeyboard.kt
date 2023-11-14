package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RpeKeyboard(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onRpeSelected: (rpe: Float) -> Unit,
    onClosed: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 100)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 100)
        ) + fadeOut(),
    ) {
        ElevatedCard(
            modifier = modifier.then(
                Modifier
                .fillMaxWidth()
                .height(LocalConfiguration.current.screenHeightDp.dp.times(.30f))),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 5.dp
            ),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    modifier = Modifier
                        .padding(top = 10.dp, end = 10.dp)
                        .height(55.dp)
                        .width(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50.dp)
                        ),
                    onClick = {
                        focusManager.clearFocus()
                        onClosed()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(25.dp),
                        imageVector = Icons.Filled.Check,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))
            }
            var selectedRpeOption: Float? by remember { mutableStateOf(null) }
            Box(modifier = Modifier.weight(.6f), contentAlignment = Alignment.BottomCenter) {
                if (selectedRpeOption != null) {
                    val repsLeftInTank = 10.0 - selectedRpeOption!!
                    val repsLeftInTankText = if (repsLeftInTank.toInt().toDouble() == repsLeftInTank) {
                        if (repsLeftInTank > 1) {
                            "You could comfortably perform ${repsLeftInTank.toInt()} more reps before failure."
                        } else if (repsLeftInTank == 1.0) {
                            "You could comfortably perform ${repsLeftInTank.toInt()} more rep before failure."
                        }
                        else {
                            "Maximal exertion. No more reps possible."
                        }
                    }
                    else {
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
                }
                else {
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
                    val rpeMinValue = 6f
                    val rpeMaxValue = 10f
                    for (rpe in ((rpeMinValue * 2).toInt()..(rpeMaxValue * 2).toInt() step 1).map { it / 2f }) {
                        RpeOption(
                            modifier = Modifier.weight(1f),
                            isSelected = selectedRpeOption == rpe,
                            value = rpe,
                            isFirst = rpe == rpeMinValue,
                            isLast = rpe == rpeMaxValue,
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

    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable { selected() },
        color = if(isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(
                topStart = if (isFirst) 16.dp else 0.dp,
                topEnd = if (isLast) 16.dp else 0.dp,
                bottomStart = if (isFirst) 16.dp else 0.dp,
                bottomEnd = if (isLast) 16.dp else 0.dp,)
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
                color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
