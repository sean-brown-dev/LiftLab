package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.ui.viewmodels.HomeViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.EditWorkoutScreen
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.composables.SectionLabel
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

    LazyColumn(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
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
}