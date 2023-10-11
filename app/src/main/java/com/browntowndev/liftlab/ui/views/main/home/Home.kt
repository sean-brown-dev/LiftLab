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
import com.browntowndev.liftlab.ui.viewmodels.HomeViewModel
import com.browntowndev.liftlab.ui.views.composables.rememberMarker
import com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails.SectionLabel
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(paddingValues: PaddingValues) {
    val homeViewModel: HomeViewModel = koinViewModel()
    val state by homeViewModel.state.collectAsState()

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

            SectionLabel(
                modifier = Modifier.padding(top = 10.dp),
                text = "HISTORY",
                fontSize = 14.sp,
            )
        }

        items(state.dateOrderedWorkoutLogs, { it.historicalWorkoutNameId }) {
            WorkoutHistoryCard(
                workoutLogEntryId = it.id,
                workoutName = it.workoutName,
                workoutDate = it.date,
                workoutDuration = it.durationInMillis,
                setResults = it.setResults,
                topSets = state.topSets,
            )
        }
    }
}