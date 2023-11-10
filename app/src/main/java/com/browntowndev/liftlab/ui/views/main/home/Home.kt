package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.viewmodels.HomeViewModel
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.composables.RowMultiSelect
import com.browntowndev.liftlab.ui.views.composables.rememberMarker
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun Home(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
) {
    val homeViewModel: HomeViewModel = koinViewModel {
        parametersOf(navHostController)
    }
    val state by homeViewModel.state.collectAsState()

    homeViewModel.registerEventBus()
    EventBusDisposalEffect(
        navHostController = navHostController,
        viewModelToUnregister = homeViewModel
    )

    Box(contentAlignment = Alignment.BottomCenter) {
        LazyColumn(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(15.dp),
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
                    Spacer(modifier = Modifier.height(15.dp))
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
                        marker = rememberMarker(),
                    )
                }
            }
        }

        RowMultiSelect(
            visible = state.showLiftChartPicker,
            title = "ADD LIFT METRIC CHARTS",
            options = listOf(
                LiftMetricChartType.ESTIMATED_ONE_REP_MAX.displayName(),
                LiftMetricChartType.VOLUME.displayName(),
                LiftMetricChartType.RELATIVE_INTENSITY.displayName()
            ),
            selections = state.liftChartTypeSelections,
            onSelectionChanged = { type, selected ->
                homeViewModel.updateLiftChartTypeSelections(
                    type = type,
                    selected = selected
                )
            },
            onCancel = { homeViewModel.toggleLiftChartPicker() }
        ) {
            TextButton(onClick = { homeViewModel.selectLiftForMetricCharts() }) {
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Select Lift",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                    )
                }
            }
        }
    }
}