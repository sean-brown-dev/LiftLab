package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.browntowndev.liftlab.ui.views.composables.MultiLineChart
import com.browntowndev.liftlab.ui.views.composables.SectionLabel
import com.browntowndev.liftlab.ui.views.composables.rememberLegend
import com.browntowndev.liftlab.ui.views.composables.rememberMarker
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberEndAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.chart.composed.plus
import com.patrykandpatrick.vico.core.chart.copy
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll


@Composable
fun MultiLineWorkoutFilterableChart(
    label: String,
    volumeChartModel: ComposedChartModel?,
    workoutFilterOptions: Map<Long, String>,
    selectedWorkoutFilters: Set<Long>,
    onApplyWorkoutFilters: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    Card(
        modifier = Modifier.padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (volumeChartModel?.hasData == true) {
            Column(horizontalAlignment = Alignment.End) {
                Spacer(modifier = Modifier.height(5.dp))
                Row (verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(
                        modifier = Modifier.padding(top = 10.dp),
                        text = label,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 14.sp,
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
                MultiLineChart(chartModel = volumeChartModel)
            }
        }
    }
}