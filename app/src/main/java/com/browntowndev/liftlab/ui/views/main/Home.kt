package com.browntowndev.liftlab.ui.views.main

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.viewmodels.HomeScreenViewModel
import com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails.SectionLabel
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
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(paddingValues: PaddingValues) {
    val homeScreenViewModel: HomeScreenViewModel = koinViewModel()
    val state by homeScreenViewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        ColumnChart(label = "WEEKLY COMPLETED WORKOUTS", chartModel = state.weeklyCompletionChart)
        ColumnChart(label = "PERCENTAGE OF SETS COMPLETED", chartModel = state.microCycleCompletionChart)
    }
}

@Composable
private fun ColumnChart(
    label: String,
    chartModel: ChartModel,
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
                ProvideChartStyle(m3ChartStyle()) {
                    val defaultColumns = currentChartStyle.columnChart.columns
                    Chart(
                        modifier = Modifier
                            .height(200.dp)
                            .padding(top = 5.dp, bottom = 10.dp),
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