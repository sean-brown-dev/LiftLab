package com.browntowndev.liftlab.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.core.DefaultAlpha
import com.patrykandpatrick.vico.core.DefaultColors
import com.patrykandpatrick.vico.core.Defaults.COLUMN_ROUNDNESS_PERCENT
import com.patrykandpatrick.vico.core.chart.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders

@Composable
internal fun rememberChartStyle(
    columnChartColors: List<Color> = listOf(),
    lineChartColors: List<Color> = listOf()
): ChartStyle {
    return remember(columnChartColors, lineChartColors) {
        ChartStyle(
            ChartStyle.Axis(
                axisLabelColor = Color(DefaultColors.Dark.axisLabelColor),
                axisGuidelineColor = Color(DefaultColors.Dark.axisGuidelineColor),
                axisLineColor = Color(DefaultColors.Dark.axisLineColor),
            ),
            ChartStyle.ColumnLayer(
                columnChartColors.map { columnChartColor ->
                    LineComponent(
                        color = columnChartColor.toArgb(),
                        shape = Shapes.roundedCornerShape(40),
                    )
                },
            ),
            ChartStyle.LineLayer(
                lineChartColors.map { lineChartColor ->
                    LineCartesianLayer.LineSpec(
                        shader = DynamicShaders.fromBrush(
                            Brush.verticalGradient(listOf(lineChartColor))
                        ),
                        backgroundShader = DynamicShaders.fromBrush(
                            Brush.verticalGradient(
                                listOf(
                                    lineChartColor.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_START),
                                    lineChartColor.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_END),
                                ),
                            ),
                        ),
                    )
                },
            ),
            ChartStyle.Marker(),
            Color(DefaultColors.Dark.elevationOverlayColor),
        )
    }
}

@Composable
internal fun rememberChartStyle(chartColors: List<Color>) =
    rememberChartStyle(columnChartColors = chartColors, lineChartColors = chartColors)