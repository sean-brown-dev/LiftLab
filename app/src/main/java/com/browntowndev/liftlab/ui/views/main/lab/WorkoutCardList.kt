package com.browntowndev.liftlab.ui.views.main.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto


@Composable
fun WorkoutCardList(
    paddingValues: PaddingValues,
    navigationController: NavHostController,
    workouts: List<WorkoutDto>,
    showEditWorkoutNameModal: (WorkoutDto) -> Unit,
    beginDeleteWorkout: (WorkoutDto) -> Unit,
) {
    val listState = rememberLazyListState()
    var workoutsState = remember { workouts }
    var workoutCount = remember { workoutsState.count() }

    LaunchedEffect(key1 = workouts) {
        workoutsState = workouts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.Center)
            .padding(paddingValues),
        contentPadding = PaddingValues(8.dp),
        state = listState,
    ) {
        items(workouts, { it.id }) { workout ->
            WorkoutCard(
                workoutName = workout.name,
                workoutId = workout.id,
                lifts = workout.lifts,
                navigationController = navigationController,
                showEditWorkoutNameModal = { showEditWorkoutNameModal(workout) },
                beginDeleteWorkout = { beginDeleteWorkout(workout) },
            )
        }
    }

    LaunchedEffect(key1 = workouts) {
        if(workouts.count() > workoutCount) {
            listState.animateScrollToItem(workoutCount)
        }

        workoutCount = workoutsState.count()
    }
}