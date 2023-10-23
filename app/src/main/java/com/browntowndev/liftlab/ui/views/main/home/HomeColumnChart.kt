package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.views.composables.rememberChartStyle
import com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails.SectionLabel
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
fun HomeColumnChart(
    modifier: Modifier = Modifier,
    label: String,
    chartModel: ChartModel,
    marker: Marker? = null,
) {
    SectionLabel(
        modifier = Modifier.padding(top = 10.dp),
        text = label,
        fontSize = 14.sp,
    )
    Card(
        modifier = Modifier.padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (chartModel.hasData) {
            val scrollState = rememberChartScrollState()
            val scrollSpec = rememberChartScrollSpec(
                isScrollEnabled = true,
                autoScrollAnimationSpec = tween(0),
                autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
                initialScroll = InitialScroll.End
            )
            Column(horizontalAlignment = Alignment.End) {
                ProvideChartStyle(rememberChartStyle(chartColors = listOf(MaterialTheme.colorScheme.secondary))) {
                    val defaultColumns = currentChartStyle.columnChart.columns
                    Chart(
                        modifier = modifier,
                        marker = marker,
                        chart = columnChart(
                            columns = remember(defaultColumns) {
                                defaultColumns.map { defaultColumn ->
                                    LineComponent(defaultColumn.color, 10.dp.value, Shapes.roundedCornerShape(topLeftPercent = 15, topRightPercent = 15))
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
        }
    }
}