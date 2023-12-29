package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.browntowndev.liftlab.ui.models.OneRepMaxEntry
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel

data class LiftDetailsState(
    val lift: LiftDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
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