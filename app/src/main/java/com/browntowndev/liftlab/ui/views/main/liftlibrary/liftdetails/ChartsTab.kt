package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import android.graphics.Typeface
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberEndAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.legend.legendItem
import com.patrykandpatrick.vico.compose.legend.verticalLegend
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
fun ChartsTab(
    oneRepMaxChartModel: ChartModel,
    volumeChartModel: ComposedChartModel,
    intensityChartModel: ChartModel,
    workoutFilterOptions: Map<Long, String>,
    selectedOneRepMaxWorkoutFilters: Set<Long>,
    selectedVolumeWorkoutFilters: Set<Long>,
    selectedIntensityWorkoutFilters: Set<Long>,
    onFilterOneRepMaxChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
    onFilterVolumeChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
    onFilterIntensityChartByWorkouts: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleLineChart(
            label = remember(oneRepMaxChartModel) { "ESTIMATED ONE REP MAX ${if(!oneRepMaxChartModel.hasData) "- NO DATA" else ""}" },
            oneRepMaxChartModel = oneRepMaxChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedOneRepMaxWorkoutFilters,
            onApplyWorkoutFilters = onFilterOneRepMaxChartByWorkouts,
        )
        MultiLineChart(
            volumeChartModel = volumeChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedVolumeWorkoutFilters,
            onApplyWorkoutFilters = onFilterVolumeChartByWorkouts,
        )
        SingleLineChart(
            label = remember(oneRepMaxChartModel) { "RELATIVE INTENSITY ${if(!oneRepMaxChartModel.hasData) "- NO DATA" else ""}" },
            oneRepMaxChartModel = intensityChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedIntensityWorkoutFilters,
            onApplyWorkoutFilters = onFilterIntensityChartByWorkouts,
        )
    }
}

@Composable
private fun SingleLineChart(
    label: String,
    oneRepMaxChartModel: ChartModel,
    workoutFilterOptions: Map<Long, String>,
    selectedWorkoutFilters: Set<Long>,
    onApplyWorkoutFilters: (historicalWorkoutIds: Set<Long>) -> Unit,
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
                    WorkoutFilterDropdown(
                        selectedFilters = selectedWorkoutFilters,
                        filterOptions = workoutFilterOptions,
                        onApplyFilter = onApplyWorkoutFilters
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                ProvideChartStyle(m3ChartStyle()) {
                    val defaultLines = currentChartStyle.lineChart.lines
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val point = shapeComponent(
                        shape = Shapes.pillShape,
                        color = Color.Black,
                        margins = dimensionsOf(2.dp),
                        strokeWidth = 3.dp,
                        strokeColor = MaterialTheme.colorScheme.primary,
                    )
                    Chart(
                        modifier = Modifier
                            .height(350.dp)
                            .padding(top = 5.dp, bottom = 5.dp),
                        chart = lineChart(
                            spacing = 73.dp,
                            persistentMarkers = remember(marker) {
                                oneRepMaxChartModel.persistentMarkers?.invoke(
                                    marker
                                )
                            },
                            lines = remember(defaultLines) {
                                defaultLines.map { defaultLine ->
                                    defaultLine.copy(
                                        lineColor = primaryColor.toArgb(),
                                        point = point,
                                    )
                                }
                            },
                            axisValuesOverrider = oneRepMaxChartModel.axisValuesOverrider,
                        ),
                        model = oneRepMaxChartModel.chartEntryModel,
                        startAxis = rememberStartAxis(
                            itemPlacer = oneRepMaxChartModel.startAxisItemPlacer,
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

@Composable
private fun MultiLineChart(
    volumeChartModel: ComposedChartModel,
    workoutFilterOptions: Map<Long, String>,
    selectedWorkoutFilters: Set<Long>,
    onApplyWorkoutFilters: (historicalWorkoutIds: Set<Long>) -> Unit,
) {
    SectionLabel(
        modifier = Modifier.padding(top = 10.dp),
        text = "VOLUME ${if(!volumeChartModel.hasData) "- NO DATA" else ""}",
        fontSize = 14.sp,
    )
    Card(
        modifier = Modifier.padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        if (volumeChartModel.hasData) {
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
                    WorkoutFilterDropdown(
                        selectedFilters = selectedWorkoutFilters,
                        filterOptions = workoutFilterOptions,
                        onApplyFilter = onApplyWorkoutFilters
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                ProvideChartStyle(m3ChartStyle()) {
                    val chartColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )
                    val defaultLines = currentChartStyle.lineChart.lines
                    val primaryPoint = shapeComponent(
                        shape = Shapes.pillShape,
                        color = Color.Black,
                        margins = dimensionsOf(2.dp),
                        strokeWidth = 3.dp,
                        strokeColor = MaterialTheme.colorScheme.primary,
                    )
                    val tertiaryPoint = shapeComponent(
                        shape = Shapes.pillShape,
                        color = Color.Black,
                        margins = dimensionsOf(2.dp),
                        strokeWidth = 3.dp,
                        strokeColor = MaterialTheme.colorScheme.tertiary,
                    )
                    val startAxisLineChart = lineChart(
                        lines = remember(defaultLines) {
                            defaultLines.map { defaultLine ->
                                defaultLine.copy(
                                    lineColor = chartColors[0].toArgb(),
                                    point = primaryPoint,
                                    lineBackgroundShader = null
                                )
                            }
                        },
                        spacing = 80.dp,
                        persistentMarkers = remember(marker) {
                            volumeChartModel.persistentMarkers?.invoke(
                                marker
                            )
                        },
                        axisValuesOverrider = volumeChartModel.axisValuesOverrider,
                        targetVerticalAxisPosition = AxisPosition.Vertical.Start,
                    )
                    val endAxisLineChart = lineChart(
                        lines = remember(defaultLines) {
                            defaultLines.map { defaultLine ->
                                defaultLine.copy(
                                    lineColor = chartColors[1].toArgb(),
                                    point = tertiaryPoint,
                                    lineBackgroundShader = null
                                )
                            }
                        },
                        spacing = 80.dp,
                        persistentMarkers = remember(marker) {
                            volumeChartModel.persistentMarkers?.invoke(
                                marker
                            )
                        },
                        axisValuesOverrider = volumeChartModel.axisValuesOverrider,
                        targetVerticalAxisPosition = AxisPosition.Vertical.End,
                    )
                    Chart(
                        modifier = Modifier
                            .height(350.dp)
                            .padding(top = 5.dp, bottom = 5.dp),
                        chart = remember { startAxisLineChart + endAxisLineChart },
                        model = volumeChartModel.composedChartEntryModel,
                        startAxis = rememberStartAxis(
                            itemPlacer = volumeChartModel.startAxisItemPlacer,
                            valueFormatter = volumeChartModel.startAxisValueFormatter
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = volumeChartModel.bottomAxisValueFormatter,
                            labelRotationDegrees = volumeChartModel.bottomAxisLabelRotationDegrees
                        ),
                        endAxis = rememberEndAxis(
                            itemPlacer = volumeChartModel.endAxisItemPlacer,
                            valueFormatter = volumeChartModel.endAxisValueFormatter,
                        ),
                        marker = marker,
                        legend = rememberLegend(
                            chartColors = chartColors,
                            labels = listOf("Working Sets", "Intensity Adjusted Rep Volume")),
                        autoScaleUp = AutoScaleUp.Full,
                        chartScrollState = scrollState,
                        chartScrollSpec = scrollSpec,
                    )
                }
            }
        }
    }
}

private val legendItemLabelTextSize = 12.sp
private val legendItemIconSize = 8.dp
private val legendItemIconPaddingValue = 10.dp
private val legendItemSpacing = 4.dp
private val legendTopPaddingValue = 8.dp
private val legendPadding = dimensionsOf(top = legendTopPaddingValue)

@Composable
private fun rememberLegend(chartColors: List<Color>, labels: List<String>) = verticalLegend(
    items = chartColors.mapIndexed { index, chartColor ->
        legendItem(
            icon = shapeComponent(color = chartColor, shape = Shapes.pillShape),
            label = textComponent(
                color = currentChartStyle.axis.axisLabelColor,
                textSize = legendItemLabelTextSize,
                typeface = Typeface.MONOSPACE,
            ),
            labelText = labels[index],
        )
    },
    iconSize = legendItemIconSize,
    iconPadding = legendItemIconPaddingValue,
    spacing = legendItemSpacing,
    padding = legendPadding,
)

@Composable
private fun WorkoutFilterDropdown(
    selectedFilters: Set<Long>,
    filterOptions: Map<Long, String>,
    onApplyFilter: (historicalWorkoutIds: Set<Long>) -> Unit
) {
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
            val workoutFilters = remember(selectedFilters) {
                selectedFilters.toMutableSet()
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
                items(filterOptions.keys.toList(), { it }) { id ->
                    val currentWorkoutName = remember(id) {
                        filterOptions[id]!!
                    }
                    var isSelected by remember(
                        key1 = id,
                        key2 = selectedFilters
                    ) {
                        mutableStateOf(selectedFilters.contains(id))
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
                                if (!selectedFilters.containsAll(workoutFilters) ||
                                    !workoutFilters.containsAll(selectedFilters)
                                ) {
                                    onApplyFilter(
                                        workoutFilters.toSet()
                                    )
                                }
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
}