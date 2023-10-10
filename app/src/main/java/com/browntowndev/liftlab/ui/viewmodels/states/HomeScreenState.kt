package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.ui.util.fastMap
import arrow.core.zip
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToInt

data class HomeScreenState(
    val program: ProgramDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val dateRange: Pair<Date, Date>,
) {
    private val weeks by lazy {
        (0..7).map { i ->
            val monday = dateRange.first.toLocalDate().plusDays(i * 7L)
            val sunday = monday.plusDays(6L)
            monday to sunday
        }
    }

    val weeklyCompletionChart by lazy {
        val workoutCount = program?.workouts?.size
        val completedWorkoutsByWeek = weeks.fastMap { week ->
            week.first to
                    workoutLogs.filter { workoutLog ->
                        week.first <= workoutLog.date.toLocalDate() &&
                                workoutLog.date.toLocalDate() <= week.second
                    }.size
        }.associate { (date, completionCount) ->
            date to completionCount
        }

        val xValuesToDates = completedWorkoutsByWeek.keys.associateBy { it.toEpochDay().toFloat() }
        val chartEntryModel = entryModelOf(xValuesToDates.keys.zip(completedWorkoutsByWeek.values, ::entryOf))
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

        ChartModel(
            chartEntryModel = chartEntryModel,
            axisValuesOverrider = object: AxisValuesOverrider<ChartEntryModel> {
                override fun getMinY(model: ChartEntryModel): Float {
                    return 0f
                }
                override fun getMaxY(model: ChartEntryModel): Float {
                    return workoutCount?.toFloat() ?: 7f
                }
            },
            bottomAxisValueFormatter = { value, _ ->
                (xValuesToDates[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
            },
            startAxisValueFormatter = { value, _ ->
                value.roundToInt().toString()
            },
            startAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = (workoutCount ?: 6) + 1),
        )
    }

    val microCycleCompletionChart by lazy {
        val setCount = program?.workouts?.sumOf { workout ->
            workout.lifts.sumOf { it.setCount }
        } ?: 1

        val completedWorkoutsByWeek = workoutLogs
            .asSequence()
            .sortedByDescending { it.date }
            .take(8)
            .groupBy { "${it.mesocycle}:${it.microcycle}" }
            .map {
                it.key to it.value.sumOf { workoutLog ->
                    workoutLog.setResults.size
                }.div(setCount.toFloat())
            }.associate { (date, percentCompleted) ->
                date to percentCompleted
            }

        val xValues = List(completedWorkoutsByWeek.)
        val chartEntryModel = entryModelOf(xValues.keys.map { entryOf() })
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

        ChartModel(
            chartEntryModel = chartEntryModel,
            axisValuesOverrider = object: AxisValuesOverrider<ChartEntryModel> {
                override fun getMinY(model: ChartEntryModel): Float {
                    return 0f
                }
                override fun getMaxY(model: ChartEntryModel): Float {
                    return 1f
                }
            },
            bottomAxisValueFormatter = { value, _ ->
                (xValues[value] ?: LocalDate.ofEpochDay(value.toLong())).format(dateTimeFormatter)
            },
            startAxisValueFormatter = { value, _ ->
                "${(value * 100).roundToInt()}%"
            },
            startAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 11),
        )
    }
}
