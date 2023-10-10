package com.browntowndev.liftlab.ui.views.main

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.viewmodels.HomeScreenViewModel
import com.browntowndev.liftlab.ui.views.composables.rememberChartStyle
import com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails.SectionLabel
import com.browntowndev.liftlab.ui.views.composables.rememberMarker
import com.browntowndev.liftlab.ui.views.main.lab.WorkoutMenuDropdown
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(paddingValues: PaddingValues) {
    val homeScreenViewModel: HomeScreenViewModel = koinViewModel()
    val state by homeScreenViewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            ColumnChart(
                modifier = Modifier
                    .height(200.dp)
                    .padding(top = 5.dp),
                label = "WORKOUTS COMPLETED",
                chartModel = state.weeklyCompletionChart
            )
            Spacer(modifier = Modifier.height(15.dp))
            ColumnChart(
                modifier = Modifier
                    .height(225.dp)
                    .padding(top = 5.dp),
                label = "MICROCYCLE SETS COMPLETED",
                chartModel = state.microCycleCompletionChart,
                marker = rememberMarker(),
            )
        }

        items(state.dateOrderedWorkoutLogs, { it.historicalWorkoutNameId }) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp, horizontal = 10.dp),
                shape = CardDefaults.shape,
                border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outline),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                onClick = { /*TODO*/ }
            ) {
                Text(
                    text = it.workoutName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(15.dp, 0.dp, 0.dp, 0.dp)
                )
                Divider(thickness = 12.dp, color = MaterialTheme.colorScheme.background)
                it.setResults.fastForEach {
                    val weight = remember(it.weight) { if (it.weight.isWholeNumber()) it.weight.roundToInt().toString() else String.format("%.2f", it.weight) }
                    Text(
                        text = "Bring Back Lift Name ${weight}x${it.reps} @${it.rpe}",
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

@Composable
private fun ColumnChart(
    modifier: Modifier = Modifier,
    label: String,
    chartModel: ChartModel,
    marker: Marker? = null,
) {
    SectionLabel(
        modifier = Modifier.padding(top = 10.dp),
        text = label,
        fontSize = 14.sp,
    )
    Card(
        modifier = Modifier.padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (chartModel.hasData) {
            val scrollState = rememberChartScrollState()
            val scrollSpec = rememberChartScrollSpec(
                isScrollEnabled = true,
                autoScrollAnimationSpec = tween(0),
                autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
                initialScroll = InitialScroll.End
            )
            Column(horizontalAlignment = Alignment.End) {
                ProvideChartStyle(rememberChartStyle(chartColors = listOf(MaterialTheme.colorScheme.secondary))) {
                    val defaultColumns = currentChartStyle.columnChart.columns
                    Chart(
                        modifier = modifier,
                        marker = marker,
                        chart = columnChart(
                            columns = remember(defaultColumns) {
                                defaultColumns.map { defaultColumn ->
                                    LineComponent(defaultColumn.color, 10.dp.value, defaultColumn.shape)
                                }
                            },
                            axisValuesOverrider = chartModel.axisValuesOverrider,
                        ),
                        model = chartModel.chartEntryModel,
                        startAxis = rememberStartAxis(
                            itemPlacer = chartModel.startAxisItemPlacer,
                            valueFormatter = chartModel.startAxisValueFormatter
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = chartModel.bottomAxisValueFormatter,
                            labelRotationDegrees = chartModel.bottomAxisLabelRotationDegrees
                        ),
                        autoScaleUp = AutoScaleUp.Full,
                        chartScrollState = scrollState,
                        chartScrollSpec = scrollSpec,
                    )
                }
            }
        }
    }
}