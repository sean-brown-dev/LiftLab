package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.runtime.Composable
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.browntowndev.liftlab.ui.composables.MultiLineChart
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel

@Composable
fun HomeMultiLineChart(
    chartModel: ComposedChartModel<LineCartesianLayerModel>,
    label: String,
    onDelete: () -> Unit,
) {
    ChartCard(label = label, onDelete = onDelete) {
        MultiLineChart(chartModel = chartModel)
    }
}