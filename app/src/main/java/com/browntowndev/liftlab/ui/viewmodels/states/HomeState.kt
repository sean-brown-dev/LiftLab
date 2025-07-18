package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.VolumeMetricChartDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricOptionTree
import com.browntowndev.liftlab.ui.models.VolumeMetricChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel

data class HomeState(
    val activeProgram: ProgramDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = emptyList(),
    val lifts: List<LiftDto> = emptyList(),
    val workoutCompletionChart: ChartModel<ColumnCartesianLayerModel>? = null,
    val microCycleCompletionChart: ChartModel<ColumnCartesianLayerModel>? = null,
    val showLiftChartPicker: Boolean = false,
    val volumeMetricCharts: List<VolumeMetricChartDto> = emptyList(),
    val volumeTypeSelections: List<String> = emptyList(),
    val volumeImpactSelection: String? = null,
    val liftChartTypeSelections: List<String> = emptyList(),
    val liftMetricOptions: LiftMetricOptionTree? = null,
    val liftMetricCharts: List<LiftMetricChartDto> = emptyList(),
    val liftMetricChartModels: List<LiftMetricChartModel> = emptyList(),
    val volumeMetricChartModels: List<VolumeMetricChartModel> = emptyList(),
    val loginModalVisible: Boolean = false,
    val firebaseUsername: String? = null,
    val emailVerified: Boolean = false,
    val firebaseError: String? = null,
) {
    val activeProgramName: String
        get() = activeProgram?.name ?: ""

    val loggedIn: Boolean
        get() = firebaseUsername != null

    val chartSelections: List<String> by lazy {
        volumeTypeSelections.toMutableList().apply {
            addAll(liftChartTypeSelections)
            if (volumeImpactSelection != null) {
                add(volumeImpactSelection)
            }
        }
    }
}
