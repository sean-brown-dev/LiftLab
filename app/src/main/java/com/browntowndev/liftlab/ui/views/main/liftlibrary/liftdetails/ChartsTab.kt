package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel

@Composable
fun ChartsTab(
    oneRepMaxChartModel: ChartModel<LineCartesianLayerModel>?,
    volumeChartModel: ComposedChartModel<LineCartesianLayerModel>?,
    intensityChartModel: ChartModel<LineCartesianLayerModel>?,
    workoutFilterOptions: Map<Long, String>,
    selectedOneRepMaxWorkoutFilters: Set<Long>,
    selectedVolumeWorkoutFilters: Set<Long>,
    selectedIntensityWorkoutFilters: Set<Long>,
    onFilterOneRepMaxChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
    onFilterVolumeChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
    onFilterIntensityChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SingleLineWorkoutFilterableChart(
                label = remember(oneRepMaxChartModel) { LiftMetricChartType.ESTIMATED_ONE_REP_MAX.displayName() },
                oneRepMaxChartModel = oneRepMaxChartModel,
                workoutFilterOptions = workoutFilterOptions,
                selectedWorkoutFilters = selectedOneRepMaxWorkoutFilters,
                onApplyWorkoutFilters = onFilterOneRepMaxChartByWorkouts,
            )
        }
        item {
            MultiLineWorkoutFilterableChart(
                label = remember(volumeChartModel) { LiftMetricChartType.VOLUME.displayName() },
                volumeChartModel = volumeChartModel,
                workoutFilterOptions = workoutFilterOptions,
                selectedWorkoutFilters = selectedVolumeWorkoutFilters,
                onApplyWorkoutFilters = onFilterVolumeChartByWorkouts,
            )
        }
        item {
            SingleLineWorkoutFilterableChart(
                label = remember(oneRepMaxChartModel) { LiftMetricChartType.RELATIVE_INTENSITY.displayName() },
                oneRepMaxChartModel = intensityChartModel,
                workoutFilterOptions = workoutFilterOptions,
                selectedWorkoutFilters = selectedIntensityWorkoutFilters,
                onApplyWorkoutFilters = onFilterIntensityChartByWorkouts,
            )
        }
    }
}