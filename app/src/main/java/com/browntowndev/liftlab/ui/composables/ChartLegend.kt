package com.browntowndev.liftlab.ui.composables

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.rememberLegendItem
import com.patrykandpatrick.vico.compose.common.rememberVerticalLegend
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.DrawContext
import com.patrykandpatrick.vico.core.common.MeasureContext
import com.patrykandpatrick.vico.core.common.shape.Shape


private val legendItemLabelTextSize = 12.sp
private val legendItemIconSize = 8.dp
private val legendItemIconPaddingValue = 10.dp
private val legendItemSpacing = 4.dp
private val legendTopPaddingValue = 8.dp
private val legendPadding = Dimensions(legendTopPaddingValue.value)

@Composable
fun<M : MeasureContext, D : DrawContext> rememberLegend(chartColors: List<Color>, labels: List<String>) =
    rememberVerticalLegend<M, D>(
        items = chartColors.mapIndexed { index, chartColor ->
            rememberLegendItem(
                icon = rememberShapeComponent(color = chartColor, shape = Shape.Pill),
                labelComponent = rememberTextComponent(
                    color = MaterialTheme.colorScheme.primary,
                    textSize = legendItemLabelTextSize,
                    typeface = Typeface.MONOSPACE,
                ),
                label = labels[index]
            )
        },
        iconSize = legendItemIconSize,
        iconPadding = legendItemIconPaddingValue,
        spacing = legendItemSpacing,
        padding = legendPadding
    )