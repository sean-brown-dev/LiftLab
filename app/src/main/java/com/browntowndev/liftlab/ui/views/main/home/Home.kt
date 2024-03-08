package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.browntowndev.liftlab.ui.viewmodels.HomeViewModel
import com.browntowndev.liftlab.ui.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.composables.RowMultiSelect
import com.browntowndev.liftlab.ui.composables.rememberMarker
import com.patrykandpatrick.vico.core.model.LineCartesianLayerModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Home(
    paddingValues: PaddingValues,
    screenId: String?,
    setTopAppBarCollapsed: (Boolean) -> Unit,
    onNavigateToSettingsMenu: () -> Unit,
    onNavigateToLiftLibrary: (chartIds: List<Long>) -> Unit,
) {
    val homeViewModel: HomeViewModel = koinViewModel {
        parametersOf(onNavigateToSettingsMenu, onNavigateToLiftLibrary)
    }
    val state by homeViewModel.state.collectAsState()

    homeViewModel.registerEventBus()
    EventBusDisposalEffect(
        screenId = screenId,
        viewModelToUnregister = homeViewModel
    )

    Box(contentAlignment = Alignment.BottomCenter) {
        LazyColumn(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = { homeViewModel.toggleLiftChartPicker() }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(R.string.add_lift_chart),
                        )
                    }
                }
            }
            item {
                if (state.workoutCompletionChart != null) {
                    HomeColumnChart(
                        modifier = Modifier
                            .height(200.dp)
                            .padding(top = 5.dp),
                        label = "WORKOUTS COMPLETED",
                        chartModel = state.workoutCompletionChart!!
                    )
                }
            }
            item {
                if (state.microCycleCompletionChart != null) {
                    HomeColumnChart(
                        modifier = Modifier
                            .height(225.dp)
                            .padding(top = 5.dp),
                        label = "MICROCYCLE SETS COMPLETED",
                        chartModel = state.microCycleCompletionChart!!,
                        marker = rememberMarker(MaterialTheme.colorScheme.primary),
                    )
                }
            }
            items(state.volumeMetricChartModels, key = { it.id }) { chart ->
                val label = remember(chart.volumeType, chart.volumeTypeImpact) {
                    "${chart.volumeType} - ${chart.volumeTypeImpact}".uppercase()
                }
                when (val chartModel = chart.chartModel) {
                    is ComposedChartModel<LineCartesianLayerModel> -> HomeMultiLineChart(
                        chartModel = chartModel,
                        label = label,
                        onDelete = {
                            homeViewModel.deleteVolumeMetricChart(id = chart.id)
                        },
                    )
                    else -> throw Exception("Unrecognized volume chart type: ${chartModel::class.simpleName}")
                }
            }
            items(state.liftMetricChartModels, key = { it.id }) { chart ->
                val label = remember(chart.liftName, chart.type) {
                    "${chart.liftName} - ${chart.type.displayName()}".uppercase()
                }
                when (val chartModel = chart.chartModel) {
                    is ChartModel<LineCartesianLayerModel> -> HomeSingleLineChart(
                        chartModel = chartModel,
                        label = label,
                        onDelete = {
                            homeViewModel.deleteLiftMetricChart(id = chart.id)
                        },
                    )
                    is ComposedChartModel<LineCartesianLayerModel> -> HomeMultiLineChart(
                        chartModel = chartModel,
                        label = label,
                        onDelete = {
                            homeViewModel.deleteLiftMetricChart(id = chart.id)
                        },
                    )
                }
            }
        }

        if (state.liftMetricOptions != null) {
            LaunchedEffect(key1 = state.showLiftChartPicker) {
                setTopAppBarCollapsed(state.showLiftChartPicker)
            }

            RowMultiSelect(
                visible = state.showLiftChartPicker,
                title = "ADD METRIC CHARTS",
                optionTree = state.liftMetricOptions!!,
                selections = state.chartSelections,
                onCancel = { homeViewModel.toggleLiftChartPicker() }
            )
        }
    }
}