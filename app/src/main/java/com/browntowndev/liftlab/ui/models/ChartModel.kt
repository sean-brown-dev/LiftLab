package com.browntowndev.liftlab.ui.models

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.patrykandpatrick.vico.core.cartesian.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

interface BaseChartModel<T> {
    val startAxisValueOverrider: AxisValueOverrider?
    val bottomAxisValueFormatter: CartesianValueFormatter
    val startAxisValueFormatter: CartesianValueFormatter
    val persistentMarkers: ((CartesianMarker) -> Map<Float, CartesianMarker>?)?
    val startAxisItemPlacer: AxisItemPlacer.Vertical
    val bottomAxisLabelRotationDegrees: Float
}

class ChartModel<T>(
    val chartEntryModel: CartesianChartModel,
    override val startAxisValueOverrider: AxisValueOverrider?,
    override val bottomAxisValueFormatter: CartesianValueFormatter,
    override val startAxisValueFormatter: CartesianValueFormatter,
    override val persistentMarkers: ((CartesianMarker) -> Map<Float, CartesianMarker>?)? = null,
    override val startAxisItemPlacer: AxisItemPlacer.Vertical,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel<T> {
    val hasData by lazy {
        chartEntryModel.models.any()
    }
}

class ComposedChartModel<T>(
    val composedChartEntryModel: CartesianChartModel,
    override val startAxisValueOverrider: AxisValueOverrider?,
    val endAxisValueOverrider: AxisValueOverrider?,
    override val bottomAxisValueFormatter: CartesianValueFormatter,
    override val startAxisValueFormatter: CartesianValueFormatter,
    val endAxisValueFormatter: CartesianValueFormatter,
    override val persistentMarkers: ((CartesianMarker) -> Map<Float, CartesianMarker>?)? = null,
    override val startAxisItemPlacer: AxisItemPlacer.Vertical,
    val endAxisItemPlacer: AxisItemPlacer.Vertical,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel<T> {
    val hasData by lazy {
        composedChartEntryModel.models.any()
    }
}

fun getOneRepMaxChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
    workoutFilters: Set<Long>
): ChartModel<LineCartesianLayerModel> {
    val oneRepMaxesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutFilters.contains(workoutLog.historicalWorkoutNameId)
        }
        .fastMap { workoutLog ->
            workoutLog.date.toLocalDate() to
                    workoutLog.setResults.maxOf { it.oneRepMax }
        }
        .associate { (date, oneRepMax) ->
            date to oneRepMax
        }.toSortedMap()

    val xValuesToDates = oneRepMaxesByLocalDate.keys
        .mapIndexed { index, localDate -> Pair(index, localDate) }
        .associate { it.first.toFloat() to it.second }

    val chartEntryModel = if(xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = oneRepMaxesByLocalDate.values)
            }
        )
    } else CartesianChartModel.empty

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMinY(minY, toSubtract = 5)
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 9, toAdd = 5)
            }
        },
        bottomAxisValueFormatter = { value, _, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { value, _, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.count(count = { 9 }),
    )
}

fun getPerWorkoutVolumeChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
    workoutFilters: Set<Long>
): ComposedChartModel<LineCartesianLayerModel> {
    val volumesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutFilters.contains(workoutLog.historicalWorkoutNameId)
        }
        .fastMap { workoutLog ->
            val repVolume = workoutLog.setResults.sumOf { it.reps }
            val totalWeight = workoutLog.setResults.map { it.weight }.sum()
            val totalWeightIfLifting1RmEachTime = getTotalWeightIfLifting1RmEachTime(workoutLog.setResults, totalWeight)
            VolumeTypesForDate(
                date = workoutLog.date.toLocalDate(),
                workingSetVolume = workoutLog.setResults.filter { it.rpe >= 7f }.size,
                relativeVolume = repVolume * (totalWeight / totalWeightIfLifting1RmEachTime),
            )
        }.associateBy { volumes ->
            volumes.date
        }.toSortedMap()

    val xValuesToDates = volumesByLocalDate.keys
        .mapIndexed { index, localDate -> Pair(index, localDate) }
        .associate { it.first.toFloat() to it.second }

    val chartEntryModel = if (xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = volumesByLocalDate.values.map { it.workingSetVolume })
            },
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = volumesByLocalDate.values.map { it.relativeVolume })
            }
        )
    } else CartesianChartModel.empty

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ComposedChartModel(
        composedChartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        endAxisValueOverrider = object: AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        bottomAxisValueFormatter = { value, _, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { value, _, _ ->
            value.roundToInt().toString()
        },
        endAxisValueFormatter = { value, _, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.count({ 5 }),
        endAxisItemPlacer = AxisItemPlacer.Vertical.count({ 9 }),
        persistentMarkers = { null }
    )
}

fun getPerMicrocycleVolumeChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
): ComposedChartModel<LineCartesianLayerModel> {
    val volumesForEachMesoAndMicro = workoutLogs
        .groupBy { Pair(it.mesocycle, it.microcycle) } // Group by both mesocycle and microcycle
        .toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
        .asSequence()
        .associate { logsForMesoAndMicro ->
            val volumeForMicro = logsForMesoAndMicro.value.map { workoutLog ->
                workoutLog.setResults
                    .groupBy { it.liftId }
                    .values.map { liftResults ->
                        val repVolume = liftResults.sumOf { it.reps }
                        val totalWeight = liftResults.map { it.weight }.sum()
                        val totalWeightIfLifting1RmEachTime = getTotalWeightIfLifting1RmEachTime(liftResults, totalWeight)
                        val workingSetVolume = liftResults.filter { it.rpe >= 7f }.size
                        val averageIntensity = (totalWeight / totalWeightIfLifting1RmEachTime)
                        val relativeVolume = repVolume * averageIntensity

                        Pair(workingSetVolume, relativeVolume)
                    }.reduce { summedPair, currPair ->
                        Pair(summedPair.first + currPair.first, summedPair.second + currPair.second)
                    }
            }.reduce { summedPair, currPair ->
                Pair(summedPair.first + currPair.first, summedPair.second + currPair.second)
            }

            logsForMesoAndMicro.key to volumeForMicro
        }.ifEmpty { mapOf(Pair(0, 0) to Pair(0, 0f)) }

    val xValuesToMesoMicroPair = volumesForEachMesoAndMicro.keys
        .mapIndexed { index, key -> Pair(index, key) }
        .associate { it.first.toFloat() to it.second }

    val chartEntryModel = if(xValuesToMesoMicroPair.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToMesoMicroPair.keys, y = volumesForEachMesoAndMicro.values.map { it.first })
            },
            LineCartesianLayerModel.build {
                series(x = xValuesToMesoMicroPair.keys, y = volumesForEachMesoAndMicro.values.map { it.second })
            }
        )
    } else CartesianChartModel.empty

    return ComposedChartModel(
        composedChartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        endAxisValueOverrider = object: AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        bottomAxisLabelRotationDegrees = 45f,
        bottomAxisValueFormatter = { value, _, _ ->
            xValuesToMesoMicroPair[value]?.let {
                "${it.first + 1}-${it.second + 1}"
            } ?: "N/A"
        },
        startAxisValueFormatter = { value, _, _ ->
            value.roundToInt().toString()
        },
        endAxisValueFormatter = { value, _, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.count({ 5 }),
        endAxisItemPlacer = AxisItemPlacer.Vertical.count({ 9 }),
        persistentMarkers = { null }
    )
}

fun getIntensityChartModel(
    workoutLogs: List<WorkoutLogEntryDto>,
    workoutFilters: Set<Long>
): ChartModel<LineCartesianLayerModel> {
    val relativeIntensitiesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutFilters.contains(workoutLog.historicalWorkoutNameId)
        }
        .fastMap { workoutLog ->
            workoutLog.date.toLocalDate() to
                    workoutLog.setResults.maxOf {
                        it.weight / CalculationEngine.getOneRepMax(
                            if (it.weight > 0) it.weight else 1f,
                            it.reps,
                            it.rpe
                        ) * 100
                    }
        }.associate { (date, relativeIntensity) ->
            date to relativeIntensity
        }.toSortedMap()

    val xValuesToDates = relativeIntensitiesByLocalDate.keys
        .mapIndexed { index, localDate -> Pair(index, localDate) }
        .associate { it.first.toFloat() to it.second }

    val chartEntryModel = if (xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = relativeIntensitiesByLocalDate.values)
            }
        )
    } else CartesianChartModel.empty

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMinY(minY, toSubtract = 5)
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 9, toAdd = 5)
            }
        },
        bottomAxisValueFormatter = { value, _, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { value, _, _ ->
            "${String.format("%.2f", value)}%"
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.count({ 9 }),
    )
}

fun getWeeklyCompletionChart(
    workoutCompletionRange: List<Pair<LocalDate, LocalDate>>,
    workoutsInDateRange: List<WorkoutLogEntryDto>,
): ChartModel<ColumnCartesianLayerModel> {
    val completedWorkoutsByWeek = workoutCompletionRange
        .fastMap { week ->
            week.first to
                    workoutsInDateRange.filter { workoutLog ->
                        week.first <= workoutLog.date.toLocalDate() &&
                                workoutLog.date.toLocalDate() <= week.second
                    }.size
        }
        .sortedBy { it.first }
        .associate { (date, completionCount) ->
            date to completionCount
        }

    val maxWorkoutsCompleted = if (completedWorkoutsByWeek.isNotEmpty()) {
        completedWorkoutsByWeek.maxOf { it.value }
    } else 6

    val xValuesToDates = completedWorkoutsByWeek.keys
        .mapIndexed { index, localDate -> Pair(index, localDate) }
        .associate { it.first.toFloat() to it.second }

    val chartEntryModel = if (xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            ColumnCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = completedWorkoutsByWeek.values)
            }
        )
    } else CartesianChartModel.empty

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object : AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return 0f
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return maxWorkoutsCompleted.toFloat().let { if (it == 0f) 1f else it }
            }
        },
        bottomAxisValueFormatter = { value, _, _ ->
            (xValuesToDates[value]
                ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { value, _, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.count({ maxWorkoutsCompleted + 1 }),
    )
}

fun getMicroCycleCompletionChart(
    workoutLogs: List<WorkoutLogEntryDto>,
    program: ProgramDto?,
): ChartModel<ColumnCartesianLayerModel> {
    val workoutsForCurrentMeso = workoutLogs
        .filter { it.programId == program?.id && it.mesocycle == program.currentMesocycle }
        .groupBy { it.microcycle }
        .toSortedMap()
        .asSequence()
        .associate { logsForMicro ->
            val setCount = if (program?.deloadWeek == (logsForMicro.key + 1)) {
                program.workouts.sumOf { workout ->
                    workout.lifts.size * 2
                }
            } else {
                program?.workouts?.sumOf { workout ->
                    workout.lifts.sumOf { it.setCount }
                } ?: 1
            }

            logsForMicro.key + 1 to logsForMicro.value.sumOf { workoutLog ->
                workoutLog.setResults.filter { it.myoRepSetPosition == null }.size
            }.toFloat().div(setCount).times(100)
        }.ifEmpty { mapOf(1 to 0f) }

    val chartEntryModel = CartesianChartModel(
        ColumnCartesianLayerModel.build {
            series(x = workoutsForCurrentMeso.keys, y = workoutsForCurrentMeso.values)
        }
    )
    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object : AxisValueOverrider {
            override fun getMinY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return 0f
            }
            override fun getMaxY(minY: Float, maxY: Float, extraStore: ExtraStore): Float {
                return 100f
            }
        },
        bottomAxisLabelRotationDegrees = 0f,
        bottomAxisValueFormatter = { value, _, _ ->
            value.roundToInt().toString()
        },
        startAxisValueFormatter = { value, _, _ ->
            "${value.roundToInt()}%"
        },
        startAxisItemPlacer = AxisItemPlacer.Vertical.count({ 9 }),
    )
}

private fun getChartMinY(minY: Float, toSubtract: Int): Float {
    return if ((minY - toSubtract) > 0) minY - toSubtract else 0f
}

private fun getChartMaxY(maxY: Float, minY: Float, minAxisVerticalCont: Int, toAdd: Int): Float {
    val maxVal = maxY + toAdd
    val maxDifference = minAxisVerticalCont - 1
    val difference = maxVal - minY

    return if (difference < maxDifference) {
        maxVal + (maxDifference - difference)
    } else {
        maxVal
    }.let { calculatedMaxY ->
        if (calculatedMaxY <= minY) minY + 1 else calculatedMaxY
    }
}

private fun getTotalWeightIfLifting1RmEachTime(
    liftResults: List<SetLogEntryDto>,
    totalWeight: Float
) = liftResults.maxOf {
    // if 0 weight was used for all then just use 1 for each one
    // so a 1RM can be calculated
    val weight = if (totalWeight == 0f) 1f else it.weight
    CalculationEngine.getOneRepMax(weight, it.reps, it.rpe)
} * liftResults.size
