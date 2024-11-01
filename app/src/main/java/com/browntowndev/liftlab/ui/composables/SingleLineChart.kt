package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Companion.Pill

@Composable
fun SingleLineChart(
    model: ChartModel<LineCartesianLayerModel>,
) {
    val theme = rememberLiftLabChartTheme()
    val chartColors = theme.lineCartesianLayerColors

    ProvideVicoTheme(theme) {
        val marker = rememberMarker()
        val point = rememberShapeComponent(
            shape = Pill,
            color = Color.Black,
            margins = Dimensions(allDp = 2.dp.value),
            strokeThickness = 3.dp,
            strokeColor = MaterialTheme.colorScheme.primary,
        )

        CartesianChartHost(
            modifier = Modifier
                .height(350.dp)
                .padding(top = 5.dp, bottom = 5.dp),
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    pointSpacing = 70.dp,
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        listOf(
                            LineCartesianLayer.rememberLine(
                                pointProvider = LineCartesianLayer.PointProvider.single(
                                    point = LineCartesianLayer.Point(component = point)
                                )
                            )
                        )
                    ),
                    rangeProvider = remember { model.startAxisValueOverrider ?: CartesianLayerRangeProvider.auto() },
                ),
                marker = marker,
                persistentMarkers = {
                    model.persistentMarkers?.invoke(
                        marker
                    )
                },
                startAxis = VerticalAxis.rememberStart(
                    itemPlacer = model.startAxisItemPlacer,
                    valueFormatter = model.startAxisValueFormatter
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = model.bottomAxisValueFormatter,
                    labelRotationDegrees = model.bottomAxisLabelRotationDegrees,
                    size = BaseAxis.Size.Exact(75f),
                ),
            ),
            model = model.chartEntryModel,
            scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
        )
    }
}