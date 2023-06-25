package com.browntowndev.liftlab.ui.views.main.lab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.ui.viewmodels.states.screens.WorkoutBuilderScreen


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
    OutlinedCard(
        modifier = modifier
            .fillMaxSize()
            .padding(5.dp)
            .clickable {
                val workoutBuilderRoute = WorkoutBuilderScreen.navigation.route.replace("{id}", workoutId.toString())
                navigationController.navigate(workoutBuilderRoute)
            },
        shape = CardDefaults.shape,
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = workoutName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                modifier = Modifier.padding(15.dp, 0.dp, 0.dp, 0.dp)
            )
            WorkoutMenuDropdown(
                showEditWorkoutNameModal = showEditWorkoutNameModal,
                beginDeleteWorkout = beginDeleteWorkout,
            )
        }
        Divider(thickness = 12.dp, color = MaterialTheme.colorScheme.background)
        lifts.forEach {
            Text(
                text = "${it.setCount} x ${it.liftName}",
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 15.dp)
            )
        }
        Divider(thickness = 15.dp, color = MaterialTheme.colorScheme.background)
    }
}