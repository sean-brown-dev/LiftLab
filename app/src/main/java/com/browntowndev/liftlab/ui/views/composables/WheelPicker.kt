package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.pow

@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    items: List<String>,
    textSize: TextUnit = 24.sp,
    onItemSelected: (String) -> Unit,
) {
    val scrollState = rememberLazyListState()
    val itemAlphas: HashMap<Int, Float> by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                hashMapOf()
            } else {
                val fullyVisibleItemsInfo = visibleItemsInfo.toMutableList()
                val lastItem = fullyVisibleItemsInfo.last()
                val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset

                val alphaMap = hashMapOf<Int, Float>()
                if (lastItem.offset + lastItem.size > viewportHeight) {
                    val alphaAsPercentageOffScreen = (viewportHeight - lastItem.offset).toFloat() / lastItem.size
                    alphaMap[lastItem.index] = alphaAsPercentageOffScreen.pow(2)
                }

                val firstItemIfLeft = fullyVisibleItemsInfo.firstOrNull()
                if (firstItemIfLeft != null && firstItemIfLeft.offset < layoutInfo.viewportStartOffset) {
                    val alphaAsPercentageOffScreen = (1 - abs(firstItemIfLeft.offset.toFloat() / firstItemIfLeft.size))
                    alphaMap[firstItemIfLeft.index] = alphaAsPercentageOffScreen.pow(3)
                }

                alphaMap
            }
        }
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if(!scrollState.isScrollInProgress) {
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            var largestValue = 0f
            var indexToScrollTo = 0
            visibleItems.forEach {
                val currAlpha = itemAlphas[it.index] ?: 0f
                if (currAlpha > largestValue) {
                    largestValue = currAlpha
                    indexToScrollTo = it.index
                }
            }

            scrollState.scrollToItem(indexToScrollTo)
        }
    }

    val padding = remember { 15.dp }
    val height = convertTextUnitToDp(textUnit = textSize, padding) * 3
    var dialogVisible by remember { mutableStateOf(true) }
    if (dialogVisible) {
        Dialog(
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            onDismissRequest = { }
        ) {
            Column {
                Box(
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = modifier
                            .height(height)
                            .clip(RoundedCornerShape(height / 2))
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        Color.Transparent,
                                    ),
                                    radius = 700f
                                )
                            ),
                        state = scrollState
                    ) {
                        itemsIndexed(items) { index, item ->
                            Spacer(modifier = Modifier.height(25.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center){
                                Text(
                                    text = item,
                                    fontSize = textSize,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(padding)
                                        .alpha(itemAlphas[index] ?: 1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(25.dp))
                        }
                    }
                }
            }
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                TextButton(
                    modifier = Modifier.padding(bottom = 20.dp),
                    onClick = {
                        dialogVisible = false

                        val selectedIndex = scrollState.layoutInfo.visibleItemsInfo.first().index
                        val selectedPatternName = items[selectedIndex]
                        onItemSelected(selectedPatternName)
                    }
                ) {
                    Text(
                        text = "Confirm",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun convertTextUnitToDp(textUnit: TextUnit, padding: Dp): Dp {
    return with(LocalDensity.current) {
        textUnit.toDp() + (padding * 2)
    }
}