package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.workout.Program
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricOptionTree
import com.browntowndev.liftlab.ui.models.VolumeMetricChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel

data class HomeState(
    val activeProgram: Program? = null,
    val workoutLogs: List<WorkoutLogEntry> = emptyList(),
    val lifts: List<Lift> = emptyList(),
    val workoutCompletionChart: ChartModel<ColumnCartesianLayerModel>? = null,
    val microCycleCompletionChart: ChartModel<ColumnCartesianLayerModel>? = null,
    val showLiftChartPicker: Boolean = false,
    val volumeMetricCharts: List<VolumeMetricChart> = emptyList(),
    val volumeTypeSelections: List<String> = emptyList(),
    val volumeImpactSelection: String? = null,
    val liftChartTypeSelections: List<String> = emptyList(),
    val liftMetricOptions: LiftMetricOptionTree? = null,
    val liftMetricCharts: List<LiftMetricChart> = emptyList(),
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
