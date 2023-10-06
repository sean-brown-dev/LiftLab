package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import kotlin.math.roundToInt

@Composable
fun ChartsTab(
    chartModel: ChartModel,
) {
    SectionLabel(
        modifier = Modifier.padding(top = 10.dp),
        text = "ESTIMATED ONE REP MAX ${if(!chartModel.hasData) "- NO DATA" else ""}"
    )
    Card (
        modifier = Modifier.padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (chartModel.hasData) {
            val marker = rememberMarker()
            val scrollState = rememberChartScrollState()
            val scrollSpec = rememberChartScrollSpec( isScrollEnabled = true ,
                autoScrollAnimationSpec = tween(0) ,
                autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
                initialScroll = InitialScroll.End )
            ProvideChartStyle(m3ChartStyle()) {
                Chart(
                    modifier = Modifier
                        .height(350.dp)
                        .padding(top = 5.dp, bottom = 5.dp),
                    chart = lineChart(
                        spacing = 73.dp,
                        persistentMarkers = remember(marker) { chartModel.persistentMarkers(marker) },
                        axisValuesOverrider = chartModel.axisValuesOverrider,
                    ),
                    model = chartModel.chartEntryModel,
                    startAxis = rememberStartAxis(
                        itemPlacer = chartModel.itemPlacer,
                        valueFormatter = chartModel.startAxisValueFormatter
                    ),
                    bottomAxis = rememberBottomAxis(valueFormatter = chartModel.bottomAxisValueFormatter, labelRotationDegrees = chartModel.bottomAxisLabelRotationDegrees),
                    marker = marker,
                    autoScaleUp = AutoScaleUp.Full,
                    chartScrollState = scrollState,
                    chartScrollSpec = scrollSpec,
                )
            }
        }
    }
}