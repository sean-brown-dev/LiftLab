package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.browntowndev.liftlab.ui.models.OneRepMaxEntry
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class LiftDetailsState(
    val lift: LiftDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val volumeTypeDisplayNames: List<String> = listOf(),
    val secondaryVolumeTypeDisplayNames: List<String> = listOf(),
    val selectedOneRepMaxWorkoutFilters: Set<Long> = setOf(),
    val selectedVolumeWorkoutFilters: Set<Long> = setOf(),
    val selectedIntensityWorkoutFilters: Set<Long> = setOf(),
    val oneRepMax: Pair<String, String>? = null,
    val maxVolume: Pair<String, String>? = null,
    val maxWeight: Pair<String, String>? = null,
    val topTenPerformances: List<OneRepMaxEntry> = listOf(),
    val totalReps: String = "0",
    val totalVolume: String = "0",
    val workoutFilterOptions: Map<Long, String> = mapOf(),
    val oneRepMaxChartModel: ChartModel? = null,
    val volumeChartModel: ComposedChartModel? = null,
    val intensityChartModel: ChartModel? = null,
)