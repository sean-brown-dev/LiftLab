package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.composables.SingleLineChart
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel

@Composable
fun HomeSingleLineChart(
    chartModel: ChartModel<LineCartesianLayerModel>,
    label: String,
    onDelete: () -> Unit,
) {
    ChartCard(label = label, labelFontSize = 16.sp, onDelete = onDelete, color = MaterialTheme.colorScheme.primaryContainer) {
        SingleLineChart(model = chartModel)
    }
}