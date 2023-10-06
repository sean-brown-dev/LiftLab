package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import kotlin.math.roundToInt

@Composable
fun ChartsTab(
    oneRepMaxChartValues: Pair<ChartEntryModel, AxisValueFormatter<AxisPosition.Horizontal.Bottom>>,
) {

    val scrollState = rememberChartScrollState()
    val scrollSpec = rememberChartScrollSpec( isScrollEnabled = true ,
        autoScrollAnimationSpec = tween(0) ,
        autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
        initialScroll = InitialScroll.End )

    Card (modifier = Modifier.padding(10.dp)) {
        ProvideChartStyle(m3ChartStyle()) {
            Chart(
                modifier = Modifier.height(250.dp),
                chart = lineChart(spacing = 75.dp),
                model = oneRepMaxChartValues.first,
                startAxis = rememberStartAxis(valueFormatter = { value, _ ->
                    value.roundToInt().toString()
                }),
                bottomAxis = rememberBottomAxis(valueFormatter = oneRepMaxChartValues.second),
                marker = rememberMarker(),
                autoScaleUp = AutoScaleUp.Full,
                chartScrollState = scrollState,
                chartScrollSpec = scrollSpec,
            )
        }
    }
}