package com.browntowndev.liftlab.ui.composables

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.data.rememberExtraLambda
import com.patrykandpatrick.vico.compose.common.rememberVerticalLegend
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.Legend
import com.patrykandpatrick.vico.core.common.LegendItem
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Companion.Pill


private val legendItemLabelTextSize = 12.sp
private val legendItemIconSize = 8.dp
private val legendItemIconPaddingValue = 10.dp
private val legendItemSpacing = 4.dp
private val legendTopPaddingValue = 8.dp
private val legendPadding = Dimensions(legendTopPaddingValue.value)

@Composable
fun rememberLegend(chartColors: List<Color>, labels: List<String>): Legend<CartesianMeasuringContext, CartesianDrawingContext> {
    val labelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.primary,
        textSize = legendItemLabelTextSize,
        typeface = Typeface.MONOSPACE,
    )

    return rememberVerticalLegend(
        items = rememberExtraLambda {
            chartColors.mapIndexed { index, chartColor ->
                add(
                    LegendItem(
                        icon = shapeComponent(color = chartColor, shape = Pill),
                        labelComponent = labelComponent,
                        label = labels[index],
                    )
                )
            }
        },
        iconSize = legendItemIconSize,
        iconPadding = legendItemIconPaddingValue,
        spacing = legendItemSpacing,
        padding = legendPadding
    )
}