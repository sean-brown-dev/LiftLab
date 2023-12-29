package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.chart.copy
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll

@Composable
fun SingleLineChart(
    model: ChartModel<LineCartesianLayerModel>,
) {
    ProvideChartStyle(m3ChartStyle()) {
        val marker = rememberMarker()
        val defaultLines = currentChartStyle.lineLayer.lines
        val scrollState = rememberChartScrollState()
        val scrollSpec = rememberChartScrollSpec(
            isScrollEnabled = true,
            autoScrollAnimationSpec = tween(0),
            autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
            initialScroll = InitialScroll.End
        )
        val point = rememberShapeComponent(
            shape = Shapes.pillShape,
            color = Color.Black,
            margins = dimensionsOf(2.dp),
            strokeWidth = 3.dp,
            strokeColor = MaterialTheme.colorScheme.primary,
        )

        CartesianChartHost(
            modifier = Modifier
                .height(350.dp)
                .padding(top = 5.dp, bottom = 5.dp),
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    spacing = 73.dp,
                    lines = remember(defaultLines) {
                        defaultLines.map { defaultLine ->
                            defaultLine.copy(
                                point = point,
                            )
                        }
                    },
                    axisValueOverrider = model.startAxisValueOverrider,
                ),
                persistentMarkers = remember(marker) {
                    model.persistentMarkers?.invoke(
                        marker
                    )
                },
                startAxis = rememberStartAxis(
                    itemPlacer = model.startAxisItemPlacer,
                    valueFormatter = model.startAxisValueFormatter
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = model.bottomAxisValueFormatter,
                    labelRotationDegrees = model.bottomAxisLabelRotationDegrees
                ),
            ),
            model = model.chartEntryModel,
            marker = marker,
            autoScaleUp = AutoScaleUp.Full,
            chartScrollState = scrollState,
            chartScrollSpec = scrollSpec,
        )
    }
}