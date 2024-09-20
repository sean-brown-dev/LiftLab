package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Companion.Pill

@Composable
fun MultiLineChart(
    chartModel: ComposedChartModel<LineCartesianLayerModel>,
) {
    val theme = rememberLiftLabChartTheme()
    val chartColors = theme.lineCartesianLayerColors

    ProvideVicoTheme(theme) {
        val marker = rememberMarker()
        val primaryPoint = rememberShapeComponent(
            shape = Pill,
            color = Color.Black,
            margins = Dimensions(allDp = 2.dp.value),
            strokeThickness = 3.dp,
            strokeColor = MaterialTheme.colorScheme.primary,
        )
        val tertiaryPoint = rememberShapeComponent(
            shape = Pill,
            color = Color.Black,
            margins = Dimensions(allDp = 2.dp.value),
            strokeThickness = 3.dp,
            strokeColor = MaterialTheme.colorScheme.tertiary,
        )
        val startAxisLineChart = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                listOf(
                    LineCartesianLayer.rememberLine(
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            point = LineCartesianLayer.Point(component = primaryPoint)
                        )
                    )
                )
            ),
            pointSpacing = 65.dp,
            rangeProvider = remember { chartModel.startAxisValueOverrider ?: CartesianLayerRangeProvider.auto() },
            verticalAxisPosition = Axis.Position.Vertical.Start,
        )
        val endAxisLineChart = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                listOf(
                    LineCartesianLayer.rememberLine(
                        fill = remember(chartColors[1]) {
                            LineCartesianLayer. LineFill. single(fill(chartColors[1]))
                        },
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            point = LineCartesianLayer.Point(component = tertiaryPoint)
                        )
                    )
                )
            ),
            pointSpacing = 65.dp,
            rangeProvider = remember { chartModel.endAxisValueOverrider ?: CartesianLayerRangeProvider.auto() },
            verticalAxisPosition = Axis.Position.Vertical.End,
        )
        CartesianChartHost(
            modifier = Modifier
                .height(350.dp)
                .padding(top = 5.dp, bottom = 5.dp),
            chart = rememberCartesianChart(
                startAxisLineChart,
                endAxisLineChart,
                startAxis = VerticalAxis.rememberStart(
                    itemPlacer = chartModel.startAxisItemPlacer,
                    valueFormatter = chartModel.startAxisValueFormatter
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = chartModel.bottomAxisValueFormatter,
                    labelRotationDegrees = chartModel.bottomAxisLabelRotationDegrees,
                    sizeConstraint = BaseAxis.SizeConstraint.Exact(75f),
                ),
                endAxis = VerticalAxis.rememberEnd(
                    itemPlacer = chartModel.endAxisItemPlacer,
                    valueFormatter = chartModel.endAxisValueFormatter,
                ),
                legend = rememberLegend(
                    chartColors = chartColors,
                    labels = listOf("Working Sets", "Intensity Adjusted Rep Volume")
                ),
                marker = marker,
                persistentMarkers = {
                    chartModel.persistentMarkers?.invoke(
                        marker
                    )
                },
            ),
            model = chartModel.composedChartEntryModel,
            scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
        )
    }
}