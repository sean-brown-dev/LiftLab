package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.ChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.ChartScrollState
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.chart.copy
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll

@Composable
fun SingleLineChart(
    model: ChartModel,
) {
    ProvideChartStyle(m3ChartStyle()) {
        val marker = rememberMarker()
        val defaultLines = currentChartStyle.lineChart.lines
        val primaryColor = MaterialTheme.colorScheme.primary
        val scrollState = rememberChartScrollState()
        val scrollSpec = rememberChartScrollSpec(
            isScrollEnabled = true,
            autoScrollAnimationSpec = tween(0),
            autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
            initialScroll = InitialScroll.End
        )
        val point = shapeComponent(
            shape = Shapes.pillShape,
            color = Color.Black,
            margins = dimensionsOf(2.dp),
            strokeWidth = 3.dp,
            strokeColor = MaterialTheme.colorScheme.primary,
        )

        Chart(
            modifier = Modifier
                .height(350.dp)
                .padding(top = 5.dp, bottom = 5.dp),
            chart = lineChart(
                spacing = 73.dp,
                persistentMarkers = remember(marker) {
                    model.persistentMarkers?.invoke(
                        marker
                    )
                },
                lines = remember(defaultLines) {
                    defaultLines.map { defaultLine ->
                        defaultLine.copy(
                            lineColor = primaryColor.toArgb(),
                            point = point,
                        )
                    }
                },
                axisValuesOverrider = model.axisValuesOverrider,
            ),
            model = model.chartEntryModel,
            startAxis = rememberStartAxis(
                itemPlacer = model.startAxisItemPlacer,
                valueFormatter = model.startAxisValueFormatter
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = model.bottomAxisValueFormatter,
                labelRotationDegrees = model.bottomAxisLabelRotationDegrees
            ),
            marker = marker,
            autoScaleUp = AutoScaleUp.Full,
            chartScrollState = scrollState,
            chartScrollSpec = scrollSpec,
        )
    }
}