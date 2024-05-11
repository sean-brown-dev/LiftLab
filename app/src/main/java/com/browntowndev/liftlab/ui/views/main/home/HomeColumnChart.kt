package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.composables.ColumnChart
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker


@Composable
fun HomeColumnChart(
    modifier: Modifier = Modifier,
    label: String,
    subHeaderLabel: String = "",
    chartModel: ChartModel<ColumnCartesianLayerModel>,
    marker: CartesianMarker? = null,
) {
    ChartCard(
        label = label,
        subHeaderLabel = subHeaderLabel,
        labelPadding = PaddingValues(top = 14.dp, bottom = 14.dp),
        labelFontSize = 20.sp,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        if (chartModel.hasData) {
            Column(horizontalAlignment = Alignment.End) {
                ColumnChart(
                    modifier = modifier,
                    marker = marker,
                    chartModel = chartModel,
                    zoomState = rememberVicoZoomState(zoomEnabled = false),
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                )
            }
        }
    }
}
