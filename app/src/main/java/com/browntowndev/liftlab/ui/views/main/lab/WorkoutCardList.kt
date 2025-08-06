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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel


@Composable
fun WorkoutCardList(
    paddingValues: PaddingValues,
    workouts: List<WorkoutUiModel>,
    showEditWorkoutNameModal: (WorkoutUiModel) -> Unit,
    beginDeleteWorkout: (WorkoutUiModel) -> Unit,
    onNavigateToWorkoutBuilder: (workoutId: Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val workoutsState = remember(workouts) { workouts }
    val workoutCount by remember(workouts) { mutableIntStateOf(workouts.size) }
    var prevWorkoutCount by remember { mutableIntStateOf(workoutCount) }

    LaunchedEffect(key1 = workoutCount) {
        if (workoutCount > prevWorkoutCount){
            listState.animateScrollToItem(workoutCount)
        }
        prevWorkoutCount = workoutCount
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(workoutsState, { it.id }) { workout ->
            if (workout.position == 0) {
                Spacer(modifier = Modifier.height(5.dp))
            }
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
}