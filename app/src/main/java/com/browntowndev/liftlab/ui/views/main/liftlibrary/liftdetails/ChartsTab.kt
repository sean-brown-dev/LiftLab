package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.ComposedChartModel

@Composable
fun ChartsTab(
    oneRepMaxChartModel: ChartModel?,
    volumeChartModel: ComposedChartModel?,
    intensityChartModel: ChartModel?,
    workoutFilterOptions: Map<Long, String>,
    selectedOneRepMaxWorkoutFilters: Set<Long>,
    selectedVolumeWorkoutFilters: Set<Long>,
    selectedIntensityWorkoutFilters: Set<Long>,
    onFilterOneRepMaxChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
    onFilterVolumeChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
    onFilterIntensityChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleLineWorkoutFilterableChart(
            label = remember(oneRepMaxChartModel) {
                "${LiftMetricChartType.ESTIMATED_ONE_REP_MAX.displayName()} " +
                        if(oneRepMaxChartModel?.hasData != true) "- NO DATA" else ""
            },
            oneRepMaxChartModel = oneRepMaxChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedOneRepMaxWorkoutFilters,
            onApplyWorkoutFilters = onFilterOneRepMaxChartByWorkouts,
        )
        MultiLineWorkoutFilterableChart(
            label = remember(volumeChartModel) { "${LiftMetricChartType.VOLUME.displayName()} " +
                    if(volumeChartModel?.hasData != true) "- NO DATA" else ""
            },
            volumeChartModel = volumeChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedVolumeWorkoutFilters,
            onApplyWorkoutFilters = onFilterVolumeChartByWorkouts,
        )
        SingleLineWorkoutFilterableChart(
            label = remember(oneRepMaxChartModel) { "${LiftMetricChartType.RELATIVE_INTENSITY.displayName()} " +
                    if(oneRepMaxChartModel?.hasData != true) "- NO DATA" else ""
            },
            oneRepMaxChartModel = intensityChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedIntensityWorkoutFilters,
            onApplyWorkoutFilters = onFilterIntensityChartByWorkouts,
        )
    }
}