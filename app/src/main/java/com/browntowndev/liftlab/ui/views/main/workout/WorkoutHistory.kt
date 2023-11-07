package com.browntowndev.liftlab.ui.views.main.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.ui.viewmodels.WorkoutHistoryViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.screens.EditWorkoutScreen
import com.browntowndev.liftlab.ui.views.composables.EventBusDisposalEffect
import com.browntowndev.liftlab.ui.views.main.home.WorkoutHistoryCard
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkoutHistory(
    paddingValues: PaddingValues,
    navHostController: NavHostController,
) {
    val workoutHistoryViewModel: WorkoutHistoryViewModel = koinViewModel {
        parametersOf(navHostController)
    }
    val state by workoutHistoryViewModel.state.collectAsState()
    workoutHistoryViewModel.registerEventBus()
    EventBusDisposalEffect(navHostController = navHostController, viewModelToUnregister = workoutHistoryViewModel)
    
    LazyColumn(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(state.dateOrderedWorkoutLogs) { workoutLog ->
            WorkoutHistoryCard(
                workoutName = workoutLog.workoutName,
                workoutDate = workoutLog.date,
                workoutDuration = workoutLog.durationInMillis,
                setResults = workoutLog.setResults,
                topSets = state.topSets[workoutLog.id],
                onEditWorkout = {
                    val editWorkoutRoute = EditWorkoutScreen.navigation.route.replace("{workoutLogEntryId}", workoutLog.id.toString())
                    navHostController.navigate(editWorkoutRoute)
                }
            )
        }
    }
}