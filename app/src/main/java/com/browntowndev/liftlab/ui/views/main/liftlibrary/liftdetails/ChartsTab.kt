package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.scale.AutoScaleUp
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll

@Composable
fun ChartsTab(
    oneRepMaxChartModel: ChartModel,
    workoutFilterOptions: Map<Long, String>,
    selectedOneRepMaxWorkoutFilters: Set<Long>,
    onFilterOneRepMaxChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    SectionLabel(
        modifier = Modifier.padding(top = 10.dp),
        text = "ESTIMATED ONE REP MAX ${if(!oneRepMaxChartModel.hasData) "- NO DATA" else ""}"
    )
    OneRepMaxChart(
        oneRepMaxChartModel = oneRepMaxChartModel,
        workoutFilterOptions = workoutFilterOptions,
        selectedOneRepMaxWorkoutFilters = selectedOneRepMaxWorkoutFilters,
        onFilterOneRepMaxChartByWorkouts = onFilterOneRepMaxChartByWorkouts,
    )
}

@Composable
private fun OneRepMaxChart(
    oneRepMaxChartModel: ChartModel,
    workoutFilterOptions: Map<Long, String>,
    selectedOneRepMaxWorkoutFilters: Set<Long>,
    onFilterOneRepMaxChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    Card(
        modifier = Modifier.padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (oneRepMaxChartModel.hasData) {
            val marker = rememberMarker()
            val scrollState = rememberChartScrollState()
            val scrollSpec = rememberChartScrollSpec(
                isScrollEnabled = true,
                autoScrollAnimationSpec = tween(0),
                autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
                initialScroll = InitialScroll.End
            )
            Column(horizontalAlignment = Alignment.End) {
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    var filterDropdownExpanded by remember { mutableStateOf(false) }
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { filterDropdownExpanded = true },
                        painter = painterResource(id = R.drawable.filter_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(id = R.string.accessibility_filter),
                    )
                    if (filterDropdownExpanded) {
                        Dialog(onDismissRequest = { filterDropdownExpanded = false }) {
                            val workoutFilters = remember(selectedOneRepMaxWorkoutFilters) {
                                selectedOneRepMaxWorkoutFilters.toMutableSet()
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .width(250.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(15.dp)
                                    ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 10.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Filter by Workout",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 20.sp,
                                        )
                                    }
                                }
                                items(workoutFilterOptions.keys.toList(), { it }) { id ->
                                    val currentWorkoutName = remember(id) {
                                        workoutFilterOptions[id]!!
                                    }
                                    var isSelected by remember(
                                        key1 = id,
                                        key2 = selectedOneRepMaxWorkoutFilters
                                    ) {
                                        mutableStateOf(selectedOneRepMaxWorkoutFilters.contains(id))
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                    ) {
                                        Checkbox(
                                            modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                isSelected = checked
                                                if (isSelected) {
                                                    workoutFilters.add(id)
                                                } else {
                                                    workoutFilters.remove(id)
                                                }
                                            }
                                        )
                                        Text(
                                            text = currentWorkoutName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 18.sp,
                                        )
                                    }
                                }
                                item {
                                    Divider(
                                        modifier = Modifier.fillMaxWidth(),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 10.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(
                                            modifier = Modifier
                                                .padding(bottom = 5.dp, end = 5.dp),
                                            onClick = {
                                                filterDropdownExpanded = false
                                                onFilterOneRepMaxChartByWorkouts(
                                                    workoutFilters.toSet()
                                                )
                                            }
                                        ) {
                                            Text(
                                                text = "Filter",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                ProvideChartStyle(m3ChartStyle()) {
                    Chart(
                        modifier = Modifier
                            .height(350.dp)
                            .padding(top = 5.dp, bottom = 5.dp),
                        chart = lineChart(
                            spacing = 73.dp,
                            persistentMarkers = remember(marker) {
                                oneRepMaxChartModel.persistentMarkers(
                                    marker
                                )
                            },
                            axisValuesOverrider = oneRepMaxChartModel.axisValuesOverrider,
                        ),
                        model = oneRepMaxChartModel.chartEntryModel,
                        startAxis = rememberStartAxis(
                            itemPlacer = oneRepMaxChartModel.itemPlacer,
                            valueFormatter = oneRepMaxChartModel.startAxisValueFormatter
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = oneRepMaxChartModel.bottomAxisValueFormatter,
                            labelRotationDegrees = oneRepMaxChartModel.bottomAxisLabelRotationDegrees
                        ),
                        marker = marker,
                        autoScaleUp = AutoScaleUp.Full,
                        chartScrollState = scrollState,
                        chartScrollSpec = scrollSpec,
                    )
                }
            }
        }
    }
}