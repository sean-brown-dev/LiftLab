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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.DropPercentageOptions
import com.browntowndev.liftlab.core.common.enums.toDropPercentageString

@Composable
fun PercentagePicker(
    visible: Boolean,
    selectedPercentage: Float?,
    onPercentageSelected: (percentage: String) -> Unit,
) {
    CustomKeyboard(visible = visible) {
        var selectedPercentageStr by remember(selectedPercentage) {
            mutableStateOf(selectedPercentage?.toDropPercentageString() ?: "")
        }

        Box(modifier = Modifier.weight(.2f), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(if (selectedPercentageStr.isNotEmpty()) "Drop $selectedPercentageStr from the previous set's weight." else "Please select a drop percentage.")
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
                val percentageOptions = remember {
                    (DropPercentageOptions.FivePercent.wholeNumberPercentage..DropPercentageOptions.TwentyFivePercent.wholeNumberPercentage step 5)
                        .map {
                            when (it) {
                                DropPercentageOptions.FivePercent.wholeNumberPercentage -> DropPercentageOptions.FivePercent.stringPercentage
                                DropPercentageOptions.TenPercent.wholeNumberPercentage -> DropPercentageOptions.TenPercent.stringPercentage
                                DropPercentageOptions.FifteenPercent.wholeNumberPercentage -> DropPercentageOptions.FifteenPercent.stringPercentage
                                DropPercentageOptions.TwentyPercent.wholeNumberPercentage -> DropPercentageOptions.TwentyPercent.stringPercentage
                                DropPercentageOptions.TwentyFivePercent.wholeNumberPercentage -> DropPercentageOptions.TwentyFivePercent.stringPercentage
                                else -> throw Exception("$it not a recognized drop percentage. Add it to DropPercentageOptions.")
                            }
                        }
                }
                percentageOptions.fastForEach {
                    PercentageOption(
                        modifier = Modifier.weight(1f),
                        value = it,
                        isFirst = remember { it == DropPercentageOptions.FivePercent.stringPercentage },
                        isLast = remember { it == DropPercentageOptions.TwentyFivePercent.stringPercentage },
                        isSelected = remember(selectedPercentageStr) { it == selectedPercentageStr },
                        selected = {
                            selectedPercentageStr = it
                            onPercentageSelected(it)
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(.2f))
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
                text = percentage,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
