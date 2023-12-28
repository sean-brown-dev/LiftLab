package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import java.util.Locale

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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SingleLineWorkoutFilterableChart(
                label = remember(oneRepMaxChartModel) {
                    "${LiftMetricChartType.ESTIMATED_ONE_REP_MAX.displayName().uppercase(Locale.ROOT)} " +
                            if(oneRepMaxChartModel?.hasData != true) "- NO DATA" else ""
                },
                oneRepMaxChartModel = oneRepMaxChartModel,
                workoutFilterOptions = workoutFilterOptions,
                selectedWorkoutFilters = selectedOneRepMaxWorkoutFilters,
                onApplyWorkoutFilters = onFilterOneRepMaxChartByWorkouts,
            )
        }
        item {
            MultiLineWorkoutFilterableChart(
                label = remember(volumeChartModel) { "${LiftMetricChartType.VOLUME.displayName().uppercase(Locale.ROOT)} " +
                        if(volumeChartModel?.hasData != true) "- NO DATA" else ""
                },
                volumeChartModel = volumeChartModel,
                workoutFilterOptions = workoutFilterOptions,
                selectedWorkoutFilters = selectedVolumeWorkoutFilters,
                onApplyWorkoutFilters = onFilterVolumeChartByWorkouts,
            )
        }
        item {
            SingleLineWorkoutFilterableChart(
                label = remember(oneRepMaxChartModel) { "${
                    LiftMetricChartType.RELATIVE_INTENSITY.displayName().uppercase(Locale.ROOT)
                } " +
                        if(oneRepMaxChartModel?.hasData != true) "- NO DATA" else ""
                },
                oneRepMaxChartModel = intensityChartModel,
                workoutFilterOptions = workoutFilterOptions,
                selectedWorkoutFilters = selectedIntensityWorkoutFilters,
                onApplyWorkoutFilters = onFilterIntensityChartByWorkouts,
            )
        }
    }
}