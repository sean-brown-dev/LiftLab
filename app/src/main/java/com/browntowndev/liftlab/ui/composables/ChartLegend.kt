package com.browntowndev.liftlab.ui.composables

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.rememberVerticalLegend
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.common.Legend
import com.patrykandpatrick.vico.core.common.LegendItem
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Companion.Pill
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets


private val legendItemLabelTextSize = 12.sp
private val legendItemIconSize = 8.dp
private val legendTopPaddingValue = 8.dp
private val legendPadding = insets(top = legendTopPaddingValue)

@Composable
fun rememberLegend(chartColors: List<Color>, labels: List<String>): Legend<CartesianMeasuringContext, CartesianDrawingContext> {
    val labelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.primary,
        textSize = legendItemLabelTextSize,
        typeface = Typeface.MONOSPACE,
    )

    return rememberVerticalLegend(
        items = { extraStore ->
            chartColors.forEachIndexed { index, color ->
                add(
                    LegendItem(
                        shapeComponent(fill(color), Pill),
                        labelComponent,
                        labels[index],
                    )
                )
            }
        },
        iconSize = legendItemIconSize,
        padding = legendPadding
    )
}