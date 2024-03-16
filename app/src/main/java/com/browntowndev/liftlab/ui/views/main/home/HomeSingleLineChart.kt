package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.composables.SingleLineChart
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel

@Composable
fun HomeSingleLineChart(
    chartModel: ChartModel<LineCartesianLayerModel>,
    label: String,
    onDelete: () -> Unit,
) {
    ChartCard(label = label, labelFontSize = 16.sp, onDelete = onDelete) {
        SingleLineChart(model = chartModel)
    }
}