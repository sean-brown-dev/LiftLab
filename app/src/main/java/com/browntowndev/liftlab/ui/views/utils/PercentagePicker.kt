package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.DropPercentageOptions

@Composable
fun PercentagePicker(
    visible: Boolean,
    onPercentageSelected: (percentage: String) -> Unit,
) {
    var isVisible by remember { mutableStateOf(visible) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(visible) {
        isVisible = visible
    }

    AnimatedVisibility(
        visible = isVisible,
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
            modifier = Modifier
                .fillMaxWidth()
                .height(LocalConfiguration.current.screenHeightDp.dp.times(.30f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    isVisible = false
                    focusManager.clearFocus()
                }) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Filled.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }

            var selectedPercentage by remember { mutableStateOf("" ) }
            Box(modifier = Modifier.weight(.6f), contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(if(selectedPercentage.isNotEmpty()) "Drop $selectedPercentage from the previous set's weight." else "Please select a drop percentage.")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    val percentages = remember {
                        (DropPercentageOptions.FivePercent.wholeNumberPercentage..DropPercentageOptions.TwentyFivePercent.wholeNumberPercentage step 5)
                            .map {
                                when(it) {
                                    DropPercentageOptions.FivePercent.wholeNumberPercentage -> DropPercentageOptions.FivePercent.stringPercentage
                                    DropPercentageOptions.TenPercent.wholeNumberPercentage -> DropPercentageOptions.TenPercent.stringPercentage
                                    DropPercentageOptions.FifteenPercent.wholeNumberPercentage -> DropPercentageOptions.FifteenPercent.stringPercentage
                                    DropPercentageOptions.TwentyPercent.wholeNumberPercentage -> DropPercentageOptions.TwentyPercent.stringPercentage
                                    DropPercentageOptions.TwentyFivePercent.wholeNumberPercentage -> DropPercentageOptions.TwentyFivePercent.stringPercentage
                                    else -> throw Exception("$it not a recognized drop percentage. Add it to DropPercentageOptions.")
                                }
                            }
                    }

                    for (it in percentages) {
                        PercentageOption(
                            modifier = Modifier.weight(1f),
                            value = it,
                            isFirst = it == DropPercentageOptions.FivePercent.stringPercentage,
                            isLast = it == DropPercentageOptions.TwentyFivePercent.stringPercentage,
                            isSelected = it == selectedPercentage,
                            selected = {
                                selectedPercentage = it
                                onPercentageSelected(it)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(.2f))
        }
    }
}

@Composable
private fun PercentageOption(
    modifier: Modifier = Modifier,
    value: String,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean = false,
    selected: () -> Unit,
) {
    val percentage by remember { mutableStateOf(value) }

    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable { selected() },
        color = if(isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
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
                text = percentage,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
