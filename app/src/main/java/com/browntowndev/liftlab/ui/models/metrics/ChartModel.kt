package com.browntowndev.liftlab.ui.models.metrics

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.domain.utils.WeightCalculationUtils
import com.browntowndev.liftlab.ui.models.workout.ProgramUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.SetLogEntryUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.WorkoutLogEntryUiModel
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

interface BaseChartModel<T> {
    val chartEntryModel: CartesianChartModel?
    val startAxisValueOverrider: CartesianLayerRangeProvider?
    val bottomAxisValueFormatter: CartesianValueFormatter
    val startAxisValueFormatter: CartesianValueFormatter
    val persistentMarkers: ((CartesianMarker) -> Map<Float, CartesianMarker>?)?
    val startAxisItemPlacer: VerticalAxis.ItemPlacer
    val bottomAxisLabelRotationDegrees: Float
}

class ChartModel<T>(
    override val chartEntryModel: CartesianChartModel?,
    override val startAxisValueOverrider: CartesianLayerRangeProvider?,
    override val bottomAxisValueFormatter: CartesianValueFormatter,
    override val startAxisValueFormatter: CartesianValueFormatter,
    override val persistentMarkers: ((CartesianMarker) -> Map<Float, CartesianMarker>?)? = null,
    override val startAxisItemPlacer: VerticalAxis.ItemPlacer,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel<T> {
    val hasData by lazy {
        chartEntryModel?.models?.any() ?: false
    }
}

class ComposedChartModel<T>(
    override val chartEntryModel: CartesianChartModel?,
    override val startAxisValueOverrider: CartesianLayerRangeProvider?,
    val endAxisValueOverrider: CartesianLayerRangeProvider?,
    override val bottomAxisValueFormatter: CartesianValueFormatter,
    override val startAxisValueFormatter: CartesianValueFormatter,
    val endAxisValueFormatter: CartesianValueFormatter,
    override val persistentMarkers: ((CartesianMarker) -> Map<Float, CartesianMarker>?)? = null,
    override val startAxisItemPlacer: VerticalAxis.ItemPlacer,
    val endAxisItemPlacer: VerticalAxis.ItemPlacer,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel<T> {
    val hasData by lazy {
        chartEntryModel?.models?.any() ?: false
    }
}

fun getOneRepMaxChartModel(
    workoutLogs: List<WorkoutLogEntryUiModel>,
    workoutFilters: Set<Long>
): ChartModel<LineCartesianLayerModel> {
    val oneRepMaxesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutLog.historicalWorkoutNameId in workoutFilters
        }
        .fastMap { workoutLog ->
            workoutLog.date.toLocalDate() to
                    workoutLog.setLogEntries.maxOf { it.oneRepMax }
        }
        .associate { (date, oneRepMax) ->
            date to oneRepMax
        }.toSortedMap()

    val xValuesToDates = oneRepMaxesByLocalDate.keys
        .mapIndexed { index, localDate -> Pair(index, localDate) }
        .associate { it.first.toDouble() to it.second }

    val chartEntryModel = if(xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = oneRepMaxesByLocalDate.values)
            }
        )
    } else null

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMinY(minY, toSubtract = 5)
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 9, toAdd = 5)
            }
        },
        bottomAxisValueFormatter = { _, value, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { _, value, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = VerticalAxis.ItemPlacer.count(count = { 9 }),
    )
}

fun getPerWorkoutVolumeChartModel(
    workoutLogs: List<WorkoutLogEntryUiModel>,
    workoutFilters: Set<Long>
): ComposedChartModel<LineCartesianLayerModel> {
    val volumesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutLog.historicalWorkoutNameId in workoutFilters
        }
        .fastMap { workoutLog ->
            val repVolume = workoutLog.setLogEntries.sumOf { it.reps }
            val totalWeight = workoutLog.setLogEntries.sumOf { it.weight.toDouble() }.toFloat()
            val totalWeightIfLifting1RmEachTime = getTotalWeightIfLifting1RmEachTime(workoutLog.setLogEntries, totalWeight)
            VolumeTypesForDate(
                date = workoutLog.date.toLocalDate(),
                workingSetVolume = workoutLog.setLogEntries.count { it.rpe >= 7f },
                relativeVolume = repVolume * (totalWeight / totalWeightIfLifting1RmEachTime),
            )
        }.associateBy { volumes ->
            volumes.date
        }.toSortedMap()

    val xValuesToDates = volumesByLocalDate.keys
        .mapIndexed { index, localDate -> Pair(index, localDate) }
        .associate { it.first.toDouble() to it.second }

    val chartEntryModel = if (xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = volumesByLocalDate.values.map { it.workingSetVolume })
            },
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = volumesByLocalDate.values.map { it.relativeVolume })
            }
        )
    } else null

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ComposedChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        endAxisValueOverrider = object: CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        bottomAxisValueFormatter = { _, value, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { _, value, _ ->
            value.roundToInt().toString()
        },
        endAxisValueFormatter = { _, value, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = VerticalAxis.ItemPlacer.count({ 5 }),
        endAxisItemPlacer = VerticalAxis.ItemPlacer.count({ 9 }),
        persistentMarkers = { null }
    )
}

fun getPerMicrocycleVolumeChartModel(
    workoutLogs: List<WorkoutLogEntryUiModel>,
    secondaryVolumeTypesByLiftId: Map<Long, Int?>?,
): ComposedChartModel<LineCartesianLayerModel> {
    val volumesForEachMesoAndMicro = workoutLogs
        .groupBy { Pair(it.mesocycle, it.microcycle) } // Group by both mesocycle and microcycle
        .toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
        .asSequence()
        .associate { logsForMesoAndMicro ->
            val volumeForMicro = logsForMesoAndMicro.value.map { workoutLog ->
                workoutLog.setLogEntries
                    .groupBy { it.liftId }
                    .map { liftResults ->
                        val repVolume = liftResults.value.sumOf { it.reps }
                        val totalWeight = liftResults.value.sumOf { it.weight.toDouble() }.toFloat()
                        val totalWeightIfLifting1RmEachTime = getTotalWeightIfLifting1RmEachTime(liftResults.value, totalWeight)

                        val workingSetVolume = liftResults.value.count { it.rpe >= 7f } /
                            if (secondaryVolumeTypesByLiftId?.contains(liftResults.key) == true) 2f else 1f

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
        .associate { it.first.toDouble() to it.second }

    val chartEntryModel = if(xValuesToMesoMicroPair.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToMesoMicroPair.keys, y = volumesForEachMesoAndMicro.values.map { it.first })
            },
            LineCartesianLayerModel.build {
                series(x = xValuesToMesoMicroPair.keys, y = volumesForEachMesoAndMicro.values.map { it.second })
            }
        )
    } else null

    return ComposedChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        endAxisValueOverrider = object: CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMinY(minY, toSubtract = 1)
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 5, toAdd = 1)
            }
        },
        bottomAxisLabelRotationDegrees = 45f,
        bottomAxisValueFormatter = { _, value, _ ->
            xValuesToMesoMicroPair[value]?.let {
                "${it.first + 1}-${it.second + 1}"
            } ?: "N/A"
        },
        startAxisValueFormatter = { _, value, _ ->
            value.roundToInt().toString()
        },
        endAxisValueFormatter = { _, value, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = VerticalAxis.ItemPlacer.count({ 5 }),
        endAxisItemPlacer = VerticalAxis.ItemPlacer.count({ 9 }),
        persistentMarkers = { null }
    )
}

fun getIntensityChartModel(
    workoutLogs: List<WorkoutLogEntryUiModel>,
    workoutFilters: Set<Long>
): ChartModel<LineCartesianLayerModel> {
    val relativeIntensitiesByLocalDate = workoutLogs
        .filter { workoutLog ->
            workoutFilters.isEmpty() ||
                    workoutLog.historicalWorkoutNameId in workoutFilters
        }
        .fastMap { workoutLog ->
            workoutLog.date.toLocalDate() to
                    workoutLog.setLogEntries.maxOf {
                        it.weight / WeightCalculationUtils.getOneRepMax(
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
        .associate { it.first.toDouble() to it.second }

    val chartEntryModel = if (xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = relativeIntensitiesByLocalDate.values)
            }
        )
    } else null

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object: CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMinY(minY, toSubtract = 5)
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return getChartMaxY(maxY, minY, minAxisVerticalCont = 9, toAdd = 5)
            }
        },
        bottomAxisValueFormatter = { _, value, _ ->
            (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { _, value, _ ->
            "${String.format(Locale.US, "%.2f", value)}%"
        },
        startAxisItemPlacer = VerticalAxis.ItemPlacer.count({ 9 }),
    )
}

fun getWeeklyCompletionChart(
    workoutCompletionRange: List<Pair<LocalDate, LocalDate>>,
    workoutsInDateRange: List<WorkoutLogEntryUiModel>,
): ChartModel<ColumnCartesianLayerModel> {
    val completedWorkoutsByWeek = workoutCompletionRange
        .fastMap { week ->
            week.first to
                    workoutsInDateRange.count { workoutLog ->
                        week.first <= workoutLog.date.toLocalDate() &&
                                workoutLog.date.toLocalDate() <= week.second
                    }
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
        .associate { it.first.toDouble() to it.second }

    val chartEntryModel = if (xValuesToDates.isNotEmpty()) {
        CartesianChartModel(
            ColumnCartesianLayerModel.build {
                series(x = xValuesToDates.keys, y = completedWorkoutsByWeek.values)
            }
        )
    } else null

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object : CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return 0.0
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return maxWorkoutsCompleted.let { if (it == 0) 1.0 else it.toDouble() }
            }
        },
        bottomAxisValueFormatter = { _, value, _ ->
            (xValuesToDates[value]
                ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
        },
        startAxisValueFormatter = { _, value, _ ->
            value.roundToInt().toString()
        },
        startAxisItemPlacer = VerticalAxis.ItemPlacer.count({ maxWorkoutsCompleted + 1 }),
    )
}

fun getMicroCycleCompletionChart(
    workoutLogs: List<WorkoutLogEntryUiModel>,
    program: ProgramUiModel?,
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
                workoutLog.setLogEntries.count { it.myoRepSetPosition == null }
            }.toFloat().div(setCount).times(100)
        }.ifEmpty { mapOf(1 to 0f) }

    val chartEntryModel = CartesianChartModel(
        ColumnCartesianLayerModel.build {
            series(x = workoutsForCurrentMeso.keys, y = workoutsForCurrentMeso.values)
        }
    )
    return ChartModel(
        chartEntryModel = chartEntryModel,
        startAxisValueOverrider = object : CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return 0.0
            }
            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                return 100.0
            }
        },
        bottomAxisLabelRotationDegrees = 0f,
        bottomAxisValueFormatter = { _, value, _ ->
            value.roundToInt().toString()
        },
        startAxisValueFormatter = { _, value, _ ->
            "${value.roundToInt()}%"
        },
        startAxisItemPlacer = VerticalAxis.ItemPlacer.count({ 9 }),
    )
}

private fun getChartMinY(minY: Double, toSubtract: Int): Double {
    return if ((minY - toSubtract) > 0) minY - toSubtract else 0.0
}

private fun getChartMaxY(maxY: Double, minY: Double, minAxisVerticalCont: Int, toAdd: Int): Double {
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
    liftResults: List<SetLogEntryUiModel>,
    totalWeight: Float
) = liftResults.maxOf {
    // if 0 weight was used for all then just use 1 for each one
    // so a 1RM can be calculated
    val weight = if (totalWeight == 0f) 1f else it.weight
    WeightCalculationUtils.getOneRepMax(weight, it.reps, it.rpe)
} * liftResults.size
