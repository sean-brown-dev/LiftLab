package com.browntowndev.liftlab.ui.views.main.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto


@Composable
fun WorkoutCardList(
    paddingValues: PaddingValues,
    workouts: List<WorkoutDto>,
    showEditWorkoutNameModal: (WorkoutDto) -> Unit,
    beginDeleteWorkout: (WorkoutDto) -> Unit,
    onNavigateToWorkoutBuilder: (workoutId: Long) -> Unit,
) {
    val listState = rememberLazyListState()
    var workoutsState = remember { workouts }
    var workoutCount = remember { workoutsState.count() }

    LaunchedEffect(key1 = workouts) {
        workoutsState = workouts
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(workouts, { it.id }) { workout ->
            WorkoutCard(
                workoutName = workout.name,
                workoutId = workout.id,
                lifts = workout.lifts,
                onNavigateToWorkoutBuilder = onNavigateToWorkoutBuilder,
                showEditWorkoutNameModal = { showEditWorkoutNameModal(workout) },
                beginDeleteWorkout = { beginDeleteWorkout(workout) },
            )
        }
        item {
            Spacer(modifier = Modifier.height(65.dp))
        }
    }

    LaunchedEffect(key1 = workouts) {
        if(workouts.count() > workoutCount) {
            listState.animateScrollToItem(workoutCount)
        }

        workoutCount = workoutsState.count()
    }
}