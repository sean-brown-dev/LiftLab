package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.VolumeMetricChartDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricOptionTree
import com.browntowndev.liftlab.ui.models.VolumeMetricChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel

data class HomeState(
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val lifts: List<LiftDto> = listOf(),
    val workoutCompletionChart: ChartModel<ColumnCartesianLayerModel>? = null,
    val microCycleCompletionChart: ChartModel<ColumnCartesianLayerModel>? = null,
    val showLiftChartPicker: Boolean = false,
    val volumeMetricCharts: List<VolumeMetricChartDto> = listOf(),
    val volumeTypeSelections: List<String> = listOf(),
    val volumeImpactSelection: String? = null,
    val liftChartTypeSelections: List<String> = listOf(),
    val liftMetricOptions: LiftMetricOptionTree? = null,
    val liftMetricChartModels: List<LiftMetricChartModel> = listOf(),
    val volumeMetricChartModels: List<VolumeMetricChartModel> = listOf(),
) {
    val chartSelections: List<String> by lazy {
        volumeTypeSelections.toMutableList().apply {
            addAll(liftChartTypeSelections)
            if (volumeImpactSelection != null) {
                add(volumeImpactSelection)
            }
        }
    }
}
