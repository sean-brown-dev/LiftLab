package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.ui.models.metrics.ChartModel
import com.browntowndev.liftlab.ui.models.metrics.ComposedChartModel
import com.browntowndev.liftlab.ui.models.workout.OneRepMaxEntry
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel

data class LiftDetailsState(
    val lift: Lift? = null,
    val workoutLogs: List<WorkoutLogEntry> = listOf(),
    val volumeTypeDisplayNames: List<String> = listOf(),
    val secondaryVolumeTypeDisplayNames: List<String> = listOf(),
    val selectedOneRepMaxWorkoutFilters: Set<Long> = setOf(),
    val selectedVolumeWorkoutFilters: Set<Long> = setOf(),
    val selectedIntensityWorkoutFilters: Set<Long> = setOf(),
    val oneRepMax: Pair<String, String>? = null,
    val maxVolume: Pair<String, String>? = null,
    val maxWeight: Pair<String, String>? = null,
    val topTenPerformances: List<OneRepMaxEntry> = listOf(),
    val totalReps: String = "0",
    val totalVolume: String = "0",
    val workoutFilterOptions: Map<Long, String> = mapOf(),
    val oneRepMaxChartModel: ChartModel<LineCartesianLayerModel>? = null,
    val volumeChartModel: ComposedChartModel<LineCartesianLayerModel>? = null,
    val intensityChartModel: ChartModel<LineCartesianLayerModel>? = null,
)