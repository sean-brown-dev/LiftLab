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
import com.browntowndev.liftlab.ui.views.composables.rememberMarker
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
    oneRepMaxChartModel: ChartModel?,
    volumeChartModel: ComposedChartModel?,
    intensityChartModel: ChartModel?,
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
            label = remember(oneRepMaxChartModel) { "ESTIMATED ONE REP MAX ${if(oneRepMaxChartModel?.hasData != true) "- NO DATA" else ""}" },
            oneRepMaxChartModel = oneRepMaxChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedOneRepMaxWorkoutFilters,
            onApplyWorkoutFilters = onFilterOneRepMaxChartByWorkouts,
        )
        MultiLineChart(
            label = remember(volumeChartModel) { "VOLUME ${if(volumeChartModel?.hasData != true) "- NO DATA" else ""}" },
            volumeChartModel = volumeChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedVolumeWorkoutFilters,
            onApplyWorkoutFilters = onFilterVolumeChartByWorkouts,
        )
        SingleLineChart(
            label = remember(oneRepMaxChartModel) { "RELATIVE INTENSITY ${if(oneRepMaxChartModel?.hasData != true) "- NO DATA" else ""}" },
            oneRepMaxChartModel = intensityChartModel,
            workoutFilterOptions = workoutFilterOptions,
            selectedWorkoutFilters = selectedIntensityWorkoutFilters,
            onApplyWorkoutFilters = onFilterIntensityChartByWorkouts,
        )
    }
}