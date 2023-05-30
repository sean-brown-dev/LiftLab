package com.browntowndev.liftlab.ui.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.viewmodels.LabViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun Lab(
    paddingValues: PaddingValues,
    labViewModel: LabViewModel = getViewModel()
) {
    val state by labViewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .wrapContentSize(Alignment.Center)
            .padding(paddingValues),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(state.programs) {program ->
            Text(
                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 8.dp),
                text = program.name,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                fontSize = 15.sp
            )

            program.workouts.forEach { workout ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    shape = CardDefaults.shape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.background)
                    Text(
                        text = workout.name,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 15.dp)
                    )
                    Divider(thickness = 12.dp, color = MaterialTheme.colorScheme.background)
                    workout.lifts.forEach {
                        Text(
                            text = "${it.setCount} x ${it.lift.name}",
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 15.dp)
                        )
                    }
                    Divider(thickness = 15.dp, color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}