package com.browntowndev.liftlab.ui.views.main.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCard(
    modifier: Modifier = Modifier,
    navigationController: NavHostController,
    workoutId: Long,
    workoutName: String,
    lifts: List<GenericWorkoutLift>,
    showEditWorkoutNameModal: () -> Unit,
    beginDeleteWorkout: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 5.dp, horizontal = 10.dp),
        shape = CardDefaults.shape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        onClick = {
            val workoutBuilderRoute = WorkoutBuilderScreen.navigation.route.replace("{id}", workoutId.toString())
            navigationController.navigate(workoutBuilderRoute)
        }
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = workoutName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 20.sp,
                modifier = Modifier.padding(15.dp, 0.dp, 0.dp, 0.dp)
            )
            WorkoutMenuDropdown(
                showEditWorkoutNameModal = showEditWorkoutNameModal,
                beginDeleteWorkout = beginDeleteWorkout,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        lifts.fastForEach {
            Text(
                text = "${it.setCount} x ${it.liftName}",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
    }
}