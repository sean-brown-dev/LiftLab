package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll

@Composable
fun ColumnChart(
    modifier: Modifier,
    marker: Marker?,
    chartModel: ChartModel
) {
    val scrollState = rememberChartScrollState()
    val scrollSpec = rememberChartScrollSpec(
        isScrollEnabled = true,
        autoScrollAnimationSpec = tween(0),
        autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
        initialScroll = InitialScroll.End
    )
    ProvideChartStyle(rememberChartStyle(chartColors = listOf(MaterialTheme.colorScheme.secondary))) {
        val defaultColumns = currentChartStyle.columnChart.columns
        Chart(
            modifier = modifier,
            marker = marker,
            chart = columnChart(
                columns = remember(defaultColumns) {
                    defaultColumns.map { defaultColumn ->
                        LineComponent(
                            defaultColumn.color,
                            10.dp.value,
                            Shapes.roundedCornerShape(topLeftPercent = 15, topRightPercent = 15)
                        )
                    }
                },
                axisValuesOverrider = chartModel.axisValuesOverrider,
            ),
            model = chartModel.chartEntryModel,
            startAxis = rememberStartAxis(
                itemPlacer = chartModel.startAxisItemPlacer,
                valueFormatter = chartModel.startAxisValueFormatter
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = chartModel.bottomAxisValueFormatter,
                labelRotationDegrees = chartModel.bottomAxisLabelRotationDegrees
            ),
            autoScaleUp = AutoScaleUp.Full,
            chartScrollState = scrollState,
            chartScrollSpec = scrollSpec,
        )
    }
}