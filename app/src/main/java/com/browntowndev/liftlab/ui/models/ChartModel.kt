package com.browntowndev.liftlab.ui.models

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.composed.ComposedChartEntryModel
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.composed.plus
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.extension.sumOf
import com.patrykandpatrick.vico.core.marker.Marker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

interface BaseChartModel {
    val axisValuesOverrider: AxisValuesOverrider<ChartEntryModel>?
    val bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>
    val startAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.Start>
    val persistentMarkers: ((Marker) -> Map<Float, Marker>?)?
    val startAxisItemPlacer: AxisItemPlacer.Vertical
    val bottomAxisLabelRotationDegrees: Float
}

class ChartModel(
    val chartEntryModel: ChartEntryModel,
    override val axisValuesOverrider: AxisValuesOverrider<ChartEntryModel>?,
    override val bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>,
    override val startAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.Start>,
    override val persistentMarkers: ((Marker) -> Map<Float, Marker>?)? = null,
    override val startAxisItemPlacer: AxisItemPlacer.Vertical,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel {
    val hasData by lazy {
        chartEntryModel.entries.any() &&
                chartEntryModel.entries.any {
                    it.any()
                }
    }
}

class ComposedChartModel(
    val composedChartEntryModel: ComposedChartEntryModel<ChartEntryModel>,
    override val axisValuesOverrider: AxisValuesOverrider<ChartEntryModel>?,
    override val bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>,
    override val startAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.Start>,
    val endAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.End>,
    override val persistentMarkers: ((Marker) -> Map<Float, Marker>?)? = null,
    override val startAxisItemPlacer: AxisItemPlacer.Vertical,
    val endAxisItemPlacer: AxisItemPlacer.Vertical,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel {
    val hasData by lazy {
        composedChartEntryModel.entries.any() &&
                composedChartEntryModel.entries.any {
                    it.any()
                }
    }
}


fun getOneRepMaxChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
    workoutFilters: Set<Long>
): ChartModel {
    val oneRepMaxesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutFilters.contains(workoutLog.historicalWorkoutNameId)
        }
        .fastMap { workoutLog ->
            workoutLog.date.toLocalDate() to
                    workoutLog.setResults.maxOf {
                        CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                    }
        }
        .associate { (date, oneRepMax) ->
            date to oneRepMax
        }.toSortedMap()

    val xValuesToDates = oneRepMaxesByLocalDate.keys.associateBy { it.toEpochDay().toFloat() }
    val chartEntryModel = entryModelOf(xValuesToDates.keys.zip(oneRepMaxesByLocalDate.values, ::entryOf))
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        axisValuesOverrider = object: AxisValuesOverrider<ChartEntryModel> {
            override fun getMinY(model: ChartEntryModel): Float {
                return model.entries.first().minOf {
                    it.y
                } - 5
            }
            override fun getMaxY(model: ChartEntryModel): Float {
                return model.entries.first().maxOf {
                    it.y
                } + 5
            }
        },
        bottomAxisValueFormatter = { value, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { value, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 10),
    )
}

fun getPerWorkoutVolumeChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
    workoutFilters: Set<Long>
): ComposedChartModel {
    val volumesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutFilters.contains(workoutLog.historicalWorkoutNameId)
        }
        .fastMap { workoutLog ->
            val repVolume = workoutLog.setResults.sumOf { it.reps.toFloat() }.roundToInt()
            val totalWeight = workoutLog.setResults.sumOf { it.weight }
            val totalWeightIfLifting1RmEachTime = workoutLog.setResults.maxOf {
                CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
            } * workoutLog.setResults.size
            VolumeTypesForDate(
                date = workoutLog.date.toLocalDate(),
                workingSetVolume = workoutLog.setResults.filter { it.rpe >= 7f }.size,
                relativeVolume = repVolume * (totalWeight / totalWeightIfLifting1RmEachTime),
            )
        }.associateBy { volumes ->
            volumes.date
        }.toSortedMap()

    val xValuesToDates = volumesByLocalDate.keys.associateBy { it.toEpochDay().toFloat() }
    val workingSetVolumeEntries = entryModelOf(xValuesToDates.keys.zip(volumesByLocalDate.map { it.value.workingSetVolume }, ::entryOf))
    val relativeVolumeEntries = entryModelOf(xValuesToDates.keys.zip(volumesByLocalDate.map { it.value.relativeVolume }, ::entryOf))
    val chartEntryModel = workingSetVolumeEntries + relativeVolumeEntries
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ComposedChartModel(
        composedChartEntryModel = chartEntryModel,
        axisValuesOverrider = object: AxisValuesOverrider<ChartEntryModel> {
            override fun getMinY(model: ChartEntryModel): Float {
                return model.entries.first().minOf {
                    it.y
                } - 1
            }
            override fun getMaxY(model: ChartEntryModel): Float {
                return model.entries.first().maxOf {
                    it.y
                } + 1
            }
        },
        bottomAxisValueFormatter = { value, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { value, _ ->
            value.roundToInt().toString()
        },
        endAxisValueFormatter = { value, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.default(
            maxItemCount =  if(workingSetVolumeEntries.entries.first().isNotEmpty()) {
                ((workingSetVolumeEntries.entries.first().maxOf { it.y } + 1) -
                        (workingSetVolumeEntries.entries.first().maxOf { it.y } - 1)).roundToInt() + 1
            } else 0
        ),
        endAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 9),
        persistentMarkers = { null }
    )
}

fun getPerMicrocycleVolumeChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
): ComposedChartModel {
    val volumesForEachMesoAndMicro = workoutLogs
        .groupBy { Pair(it.mesocycle, it.microcycle) } // Group by both mesocycle and microcycle
        .toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
        .asSequence()
        .associate { logsForMesoAndMicro ->
            val volumeForMicro = logsForMesoAndMicro.value.map { workoutLog ->
                workoutLog.setResults
                    .groupBy { it.liftId }
                    .values.map { liftResults ->
                        val repVolume = liftResults.sumOf { it.reps.toFloat() }.roundToInt()
                        val totalWeight = liftResults.sumOf { it.weight }
                        val totalWeightIfLifting1RmEachTime = liftResults.maxOf {
                            CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                        } * liftResults.size
                        val workingSetVolume = liftResults.filter { it.rpe >= 7f }.size
                        val relativeVolume = repVolume * (totalWeight / totalWeightIfLifting1RmEachTime)

                        Pair(workingSetVolume, relativeVolume)
                    }.reduce { summedPair, currPair ->
                        Pair(summedPair.first + currPair.first, summedPair.second + currPair.second)
                    }
            }.reduce { summedPair, currPair ->
                Pair(summedPair.first + currPair.first, summedPair.second + currPair.second)
            }

            logsForMesoAndMicro.key to volumeForMicro
        }.ifEmpty { mapOf(Pair(0, 0) to Pair(0, 0f)) }

    val xValuesToMesoMicroPair = volumesForEachMesoAndMicro.keys.mapIndexed { index, key -> Pair(index, key) }.associate { it.first.toFloat() to it.second }
    val workingSetVolumeEntries = entryModelOf(xValuesToMesoMicroPair.keys.zip(volumesForEachMesoAndMicro.map { it.value.first }, ::entryOf))
    val relativeVolumeEntries = entryModelOf(xValuesToMesoMicroPair.keys.zip(volumesForEachMesoAndMicro.map { it.value.second }, ::entryOf))
    val chartEntryModel = workingSetVolumeEntries + relativeVolumeEntries

    return ComposedChartModel(
        composedChartEntryModel = chartEntryModel,
        axisValuesOverrider = object: AxisValuesOverrider<ChartEntryModel> {
            override fun getMinY(model: ChartEntryModel): Float {
                return model.entries.first().minOf {
                    it.y
                } - 1
            }
            override fun getMaxY(model: ChartEntryModel): Float {
                return model.entries.first().maxOf {
                    it.y
                } + 1
            }
        },
        bottomAxisLabelRotationDegrees = 45f,
        bottomAxisValueFormatter = { value, _ ->
            xValuesToMesoMicroPair[value]?.let {
                "${it.first + 1}-${it.second + 1}"
            } ?: "N/A"
        },
        startAxisValueFormatter = { value, _ ->
            value.roundToInt().toString()
        },
        endAxisValueFormatter = { value, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.default(
            maxItemCount =  if(workingSetVolumeEntries.entries.first().isNotEmpty()) {
                ((workingSetVolumeEntries.entries.first().maxOf { it.y } + 1) -
                        (workingSetVolumeEntries.entries.first().maxOf { it.y } - 1)).roundToInt() + 1
            } else 0
        ),
        endAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 9),
        persistentMarkers = { null }
    )
}

fun getIntensityChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
    workoutFilters: Set<Long>
): ChartModel {
    val relativeIntensitiesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutFilters.contains(workoutLog.historicalWorkoutNameId)
        }
        .fastMap { workoutLog ->
            workoutLog.date.toLocalDate() to
                    workoutLog.setResults.maxOf {
                        it.weight / CalculationEngine.getOneRepMax(
                            it.weight,
                            it.reps,
                            it.rpe
                        ) * 100
                    }
        }.associate { (date, relativeIntensity) ->
            date to relativeIntensity
        }.toSortedMap()
    val xValuesToDates = relativeIntensitiesByLocalDate.keys.associateBy { it.toEpochDay().toFloat() }
    val chartEntryModel = entryModelOf(xValuesToDates.keys.zip(relativeIntensitiesByLocalDate.values, ::entryOf))
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        axisValuesOverrider = object: AxisValuesOverrider<ChartEntryModel> {
            override fun getMinY(model: ChartEntryModel): Float {
                return model.entries.first().minOf {
                    it.y
                } - 5f
            }
            override fun getMaxY(model: ChartEntryModel): Float {
                return model.entries.first().maxOf {
                    it.y
                } + 5f
            }
        },
        bottomAxisValueFormatter = { value, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { value, _ ->
            "${String.format("%.2f", value)}%"
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 10),
    )
}
