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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toSimpleDateTimeString
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.viewmodels.HomeViewModel
import com.browntowndev.liftlab.ui.views.composables.rememberChartStyle
import com.browntowndev.liftlab.ui.views.composables.rememberMarker
import com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails.SectionLabel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
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
                ColumnChart(
                    modifier = Modifier
                        .height(200.dp)
                        .padding(top = 5.dp),
                    label = "WORKOUTS COMPLETED",
                    chartModel = state.workoutCompletionChart!!
                )
                Spacer(modifier = Modifier.height(15.dp))
            }

            if (state.microCycleCompletionChart != null) {
                ColumnChart(
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
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp, horizontal = 10.dp),
                shape = CardDefaults.shape,
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                onClick = { /*TODO*/ }
            ) {
                Text(
                    text = it.workoutName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 15.dp, top = 10.dp)
                )
                Text(
                    modifier = Modifier.padding(start = 15.dp, bottom = 10.dp),
                    text = it.date.toSimpleDateTimeString(),
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 15.sp,
                )
                Row(
                    modifier = Modifier.padding(start = 15.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(15.dp),
                        painter = painterResource(id = R.drawable.stopwatch_icon),
                        tint = MaterialTheme.colorScheme.outline,
                        contentDescription = null,
                    )
                    Text(
                        text = it.durationInMillis.toTimeString(),
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 15.sp,
                    )
                }

                Row (verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        text = "Lift",
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        text = "Best Set",
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                val liftNames = remember (it) { it.setResults.distinctBy { it.liftName }.map { it.liftName } }
                liftNames.fastForEach { liftName ->
                    val topSet = remember(it) { state.topSets[liftName] }
                    if (topSet != null) {
                        val weight = remember(topSet.second.weight) {
                            if (topSet.second.weight.isWholeNumber()) topSet.second.weight.roundToInt().toString()
                            else String.format("%.2f", topSet.second.weight)
                        }
                        Row (verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${topSet.first} x ${topSet.second.liftName}",
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 15.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "$weight x ${topSet.second.reps} @${topSet.second.rpe}",
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(end = 15.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
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