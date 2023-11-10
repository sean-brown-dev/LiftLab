package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.views.composables.ColumnChart
import com.patrykandpatrick.vico.core.marker.Marker


@Composable
fun HomeColumnChart(
    modifier: Modifier = Modifier,
    label: String,
    chartModel: ChartModel,
    marker: Marker? = null,
) {
    ChartCard(label = label) {
        if (chartModel.hasData) {
            Column(horizontalAlignment = Alignment.End) {
                ColumnChart(modifier = modifier, marker = marker, chartModel = chartModel)
            }
        }
    }
}
