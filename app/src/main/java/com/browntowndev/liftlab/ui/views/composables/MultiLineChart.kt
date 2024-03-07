package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberEndAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.color
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.BaseAxis
import com.patrykandpatrick.vico.core.chart.copy
import com.patrykandpatrick.vico.core.chart.values.AxisValueOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.scroll.Scroll

@Composable
fun MultiLineChart(
    chartModel: ComposedChartModel<LineCartesianLayerModel>,
) {
    ProvideChartStyle(m3ChartStyle()) {
        val marker = rememberMarker()
        val chartColors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        )
        val defaultLines = currentChartStyle.lineLayer.lines
        val primaryPoint = rememberShapeComponent(
            shape = Shapes.pillShape,
            color = Color.Black,
            margins = dimensionsOf(2.dp),
            strokeWidth = 3.dp,
            strokeColor = MaterialTheme.colorScheme.primary,
        )
        val tertiaryPoint = rememberShapeComponent(
            shape = Shapes.pillShape,
            color = Color.Black,
            margins = dimensionsOf(2.dp),
            strokeWidth = 3.dp,
            strokeColor = MaterialTheme.colorScheme.tertiary,
        )
        val startAxisLineChart = rememberLineCartesianLayer(
            lines = remember(defaultLines) {
                defaultLines.map { defaultLine ->
                    defaultLine.copy(
                        shader = DynamicShaders.color(chartColors[0]),
                        backgroundShader = null,
                        point = primaryPoint,
                    )
                }
            },
            spacing = 65.dp,
            axisValueOverrider = remember { chartModel.startAxisValueOverrider ?: AxisValueOverrider.auto() },
            verticalAxisPosition = AxisPosition.Vertical.Start,
        )
        val endAxisLineChart = rememberLineCartesianLayer(
            lines = remember(defaultLines) {
                defaultLines.map { defaultLine ->
                    defaultLine.copy(
                        shader = DynamicShaders.color(chartColors[1]),
                        backgroundShader = null,
                        point = tertiaryPoint,
                    )
                }
            },
            spacing = 65.dp,
            axisValueOverrider = remember { chartModel.endAxisValueOverrider ?: AxisValueOverrider.auto() },
            verticalAxisPosition = AxisPosition.Vertical.End,
        )
        CartesianChartHost(
            modifier = Modifier
                .height(350.dp)
                .padding(top = 5.dp, bottom = 5.dp),
            chart = rememberCartesianChart(
                startAxisLineChart,
                endAxisLineChart,
                persistentMarkers = remember(marker) {
                    chartModel.persistentMarkers?.invoke(
                        marker
                    )
                },
                startAxis = rememberStartAxis(
                    itemPlacer = chartModel.startAxisItemPlacer,
                    valueFormatter = chartModel.startAxisValueFormatter
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = chartModel.bottomAxisValueFormatter,
                    labelRotationDegrees = chartModel.bottomAxisLabelRotationDegrees,
                    sizeConstraint = BaseAxis.SizeConstraint.Exact(75f),
                ),
                endAxis = rememberEndAxis(
                    itemPlacer = chartModel.endAxisItemPlacer,
                    valueFormatter = chartModel.endAxisValueFormatter,
                ),
                legend = rememberLegend(
                    chartColors = chartColors,
                    labels = listOf("Working Sets", "Intensity Adjusted Rep Volume")
                ),
            ),
            model = chartModel.composedChartEntryModel,
            marker = marker,
            scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
        )
    }
}