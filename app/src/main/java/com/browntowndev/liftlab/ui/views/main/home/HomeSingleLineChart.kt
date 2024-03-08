package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.runtime.Composable
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.composables.SingleLineChart
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel

@Composable
fun HomeSingleLineChart(
    chartModel: ChartModel<LineCartesianLayerModel>,
    label: String,
    onDelete: () -> Unit,
) {
    ChartCard(label = label, onDelete = onDelete) {
        SingleLineChart(model = chartModel)
    }
}