package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.common.toSimpleDateString
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.ComposedChartModel
import com.browntowndev.liftlab.ui.models.OneRepMaxEntry
import com.browntowndev.liftlab.ui.models.VolumeTypesForDate
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.composed.plus
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.extension.sumOf
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random

data class LiftDetailsState(
    val lift: LiftDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val volumeTypeDisplayNames: List<String> = listOf(),
    val secondaryVolumeTypeDisplayNames: List<String> = listOf(),
    val selectedOneRepMaxWorkoutFilters: Set<Long> = setOf(),
    val selectedVolumeWorkoutFilters: Set<Long> = setOf(),
    val selectedIntensityWorkoutFilters: Set<Long> = setOf(),
) {
    val oneRepMax: Pair<String, String>? by lazy {
        val oneRepMax = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toSimpleDateString() to
                    workoutLog.setResults.maxOf {
                        CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                    }
        }.maxByOrNull { it.second }

        if (oneRepMax != null) {
            Pair(oneRepMax.first, oneRepMax.second.toString())
        } else null
    }

    val maxVolume: Pair<String, String>? by lazy {
        val maxVolume = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toSimpleDateString() to
                    workoutLog.setResults.maxOf {
                        it.reps * it.weight
                    }
        }.maxByOrNull { it.second }

        if (maxVolume != null) {
            Pair(maxVolume.first, formatFloatString(maxVolume.second))
        } else null
    }

    val maxWeight: Pair<String, String>? by lazy {
        val maxWeight = workoutLogs.fastMap { workoutLog ->
            workoutLog.date.toSimpleDateString() to
                    workoutLog.setResults.maxOf {
                        it.weight
                    }
        }.maxByOrNull { it.second }

        if (maxWeight != null) {
            Pair(maxWeight.first, formatFloatString(maxWeight.second))
        } else null
    }

    val topTenPerformances: List<OneRepMaxEntry> by lazy {
        workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                OneRepMaxEntry(
                    setsAndRepsLabel = "${formatFloatString(setLog.weight)}x${setLog.reps} @${setLog.rpe}",
                    date = workoutLog.date.toSimpleDateString(),
                    oneRepMax = CalculationEngine.getOneRepMax(setLog.weight, setLog.reps, setLog.rpe).toString()
                )
            }
        }.sortedByDescending { it.oneRepMax }.take(10)
    }

    val totalReps: String by lazy {
        workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps
            }
        }.sum().toString()
    }

    val totalVolume: String by lazy {
        formatFloatString(workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps * setLog.weight
            }
        }.sum())
    }

    val workoutFilterOptions by lazy {
        workoutLogs.associate {
            it.historicalWorkoutNameId to it.workoutName
        }
    }

    val oneRepMaxChartModel by lazy {
        val oneRepMaxesByLocalDate = workoutLogs
            .filter { workoutLog ->
                selectedOneRepMaxWorkoutFilters.isEmpty() ||
                        selectedOneRepMaxWorkoutFilters.contains(workoutLog.historicalWorkoutNameId)
            }
            .fastMap { workoutLog ->
                workoutLog.date.toLocalDate() to
                        workoutLog.setResults.maxOf {
                            CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                        }
            }.associate { (date, oneRepMax) ->
                date to oneRepMax
            }
        val xValuesToDates = oneRepMaxesByLocalDate.keys.associateBy { it.toEpochDay().toFloat() }
        val chartEntryModel = entryModelOf(xValuesToDates.keys.zip(oneRepMaxesByLocalDate.values, ::entryOf))
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

        ChartModel(
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

    val volumeChartModel by lazy {
        val volumesByLocalDate = workoutLogs
            .filter { workoutLog ->
                selectedOneRepMaxWorkoutFilters.isEmpty() ||
                        selectedOneRepMaxWorkoutFilters.contains(workoutLog.historicalWorkoutNameId)
            }
            .fastMap { workoutLog ->
                val repVolume = workoutLog.setResults.sumOf { it.reps.toFloat() }.roundToInt()
                val totalWeight = workoutLog.setResults.sumOf { it.weight }
                VolumeTypesForDate(
                    date = workoutLog.date.toLocalDate(),
                    workingSetVolume = workoutLog.setResults.filter { it.rpe >= 7f }.size,
                    relativeVolume = repVolume *
                            (totalWeight / workoutLog.setResults.maxOf {
                            CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                        }),
                )
            }.associateBy { volumes -> volumes.date }
        val xValuesToDates = volumesByLocalDate.keys.associateBy { it.toEpochDay().toFloat() }
        val workingSetVolumeEntries = entryModelOf(xValuesToDates.keys.zip(volumesByLocalDate.map { it.value.workingSetVolume }, ::entryOf))
        val relativeVolumeEntries = entryModelOf(xValuesToDates.keys.zip(volumesByLocalDate.map { it.value.relativeVolume }, ::entryOf))
        val chartEntryModel = workingSetVolumeEntries + relativeVolumeEntries
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

        ComposedChartModel(
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
                maxItemCount = ((workingSetVolumeEntries.entries.first().maxOf { it.y } + 1) -
                        (workingSetVolumeEntries.entries.first().minOf { it.y } - 1)).roundToInt() + 1
            ),
            endAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 9),
            persistentMarkers = { null }
        )
    }

    val intensityChartModel by lazy {
        val relativeIntensitiesByLocalDate = workoutLogs
            .filter { workoutLog ->
                selectedOneRepMaxWorkoutFilters.isEmpty() ||
                        selectedOneRepMaxWorkoutFilters.contains(workoutLog.historicalWorkoutNameId)
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
            }
        val xValuesToDates = relativeIntensitiesByLocalDate.keys.associateBy { it.toEpochDay().toFloat() }
        val chartEntryModel = entryModelOf(xValuesToDates.keys.zip(relativeIntensitiesByLocalDate.values, ::entryOf))
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

        ChartModel(
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

    private fun formatFloatString(float: Float): String {
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.maximumFractionDigits = 2

        return if (float.isWholeNumber()) numberFormat.format(float.roundToInt())
        else numberFormat.format(float)
    }
}