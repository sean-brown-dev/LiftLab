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
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.core.common.shape.Shape

@Composable
fun SingleLineChart(
    model: ChartModel<LineCartesianLayerModel>,
) {
    val theme = rememberLiftLabChartTheme()
    val chartColors = theme.lineCartesianLayerColors

    ProvideVicoTheme(theme) {
        val marker = rememberMarker()
        val point = rememberShapeComponent(
            shape = Shape.Pill,
            color = Color.Black,
            margins = Dimensions(allDp = 2.dp.value),
            strokeWidth = 3.dp,
            strokeColor = MaterialTheme.colorScheme.primary,
        )

        CartesianChartHost(
            modifier = Modifier
                .height(350.dp)
                .padding(top = 5.dp, bottom = 5.dp),
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    spacing = 70.dp,
                    lines = listOf(rememberLineSpec(shader = DynamicShader.color(chartColors[0]), point = point)),
                    axisValueOverrider = remember { model.startAxisValueOverrider ?: AxisValueOverrider.auto() },
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
                    labelRotationDegrees = model.bottomAxisLabelRotationDegrees,
                    sizeConstraint = BaseAxis.SizeConstraint.Exact(75f),
                ),
            ),
            model = model.chartEntryModel,
            marker = marker,
            scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
        )
    }
}