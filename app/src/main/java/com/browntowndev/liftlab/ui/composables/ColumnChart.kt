package com.browntowndev.liftlab.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.chart.values.AxisValueOverrider
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.model.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.scroll.Scroll

@Composable
fun ColumnChart(
    modifier: Modifier,
    marker: Marker?,
    chartModel: ChartModel<ColumnCartesianLayerModel>
) {
    ProvideChartStyle(rememberChartStyle(chartColors = listOf(MaterialTheme.colorScheme.secondary))) {
        val defaultColumns = currentChartStyle.columnLayer.columns
        CartesianChartHost(
            modifier = modifier,
            marker = marker,
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columns = remember(defaultColumns) {
                        defaultColumns.map { defaultColumn ->
                            LineComponent(
                                defaultColumn.color,
                                10.dp.value,
                                Shapes.roundedCornerShape(topLeftPercent = 15, topRightPercent = 15)
                            )
                        }
                    },
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
            model = chartModel.chartEntryModel,
            scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
        )
    }
}