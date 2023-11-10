package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.runtime.Composable
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.browntowndev.liftlab.ui.views.composables.MultiLineChart

@Composable
fun HomeMultiLineChart(
    chartModel: ComposedChartModel,
    label: String,
    onDelete: () -> Unit,
) {
    ChartCard(label = label, onDelete = onDelete) {
        MultiLineChart(chartModel = chartModel)
    }
}