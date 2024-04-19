package com.browntowndev.liftlab.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.common.shape.Shape

@Composable
fun ColumnChart(
    modifier: Modifier,
    marker: CartesianMarker?,
    chartModel: ChartModel<ColumnCartesianLayerModel>,
    scrollState: VicoScrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
    zoomState: VicoZoomState = rememberVicoZoomState(),
) {
    ProvideVicoTheme(rememberLiftLabChartTheme()) {
        CartesianChartHost(
            modifier = modifier,
            marker = marker,
            scrollState = scrollState,
            zoomState = zoomState,
            model = chartModel.chartEntryModel,
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        vicoTheme.columnCartesianLayerColors.map { color ->
                            rememberLineComponent(
                                color,
                                10.dp,
                                Shape.rounded(topLeftPercent = 15, topRightPercent = 15)
                            )
                        }
                    ),
                    axisValueOverrider = remember { chartModel.startAxisValueOverrider ?: AxisValueOverrider.auto() },
                ),
                startAxis = rememberStartAxis(
                    itemPlacer = chartModel.startAxisItemPlacer,
                    valueFormatter = chartModel.startAxisValueFormatter
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = chartModel.bottomAxisValueFormatter,
                    labelRotationDegrees = chartModel.bottomAxisLabelRotationDegrees
                ),
            ),
        )
    }
}
