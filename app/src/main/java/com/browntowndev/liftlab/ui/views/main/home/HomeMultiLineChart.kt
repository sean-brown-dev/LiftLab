package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.composables.MultiLineChart
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel

@Composable
fun HomeMultiLineChart(
    chartModel: ComposedChartModel<LineCartesianLayerModel>,
    label: String,
    onDelete: () -> Unit,
) {
    ChartCard(label = label, labelFontSize = 16.sp, onDelete = onDelete, color = MaterialTheme.colorScheme.primaryContainer) {
        MultiLineChart(chartModel = chartModel)
    }
}