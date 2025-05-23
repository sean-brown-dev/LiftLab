package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.composables.SectionLabel
import com.browntowndev.liftlab.ui.composables.SingleLineChart
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel


@Composable
fun SingleLineWorkoutFilterableChart(
    label: String,
    oneRepMaxChartModel: ChartModel<LineCartesianLayerModel>?,
    workoutFilterOptions: Map<Long, String>,
    selectedWorkoutFilters: Set<Long>,
    onApplyWorkoutFilters: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    Card(
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        if (oneRepMaxChartModel?.hasData == true) {
            Column(horizontalAlignment = Alignment.End) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(
                        modifier = Modifier.padding(top = 10.dp),
                        text = label,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    WorkoutFilterDropdown(
                        selectedFilters = selectedWorkoutFilters,
                        filterOptions = workoutFilterOptions,
                        onApplyFilter = onApplyWorkoutFilters
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                SingleLineChart(model = oneRepMaxChartModel)
            }
        }
    }
}