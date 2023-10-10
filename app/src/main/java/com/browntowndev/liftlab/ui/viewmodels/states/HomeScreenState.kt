package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.ui.util.fastMap
import arrow.core.firstOrNone
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.common.toStartOfDate
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.ui.models.ChartModel
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class HomeScreenState(
    val program: ProgramDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
) {
    private val dateRange by lazy {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        monday.minusWeeks(7).toStartOfDate() to today.toEndOfDate()
    }

    private val lastSevenWeeks by lazy {
        (0..7).map { i ->
            val monday = dateRange.first.toLocalDate().plusDays(i * 7L)
            val sunday = monday.plusDays(6L)
            monday to sunday
        }
    }

    private val workoutsInDateRange by lazy {
        workoutLogs
            .filter { workoutLog ->
                dateRange.first <= workoutLog.date &&
                        workoutLog.date <= dateRange.second
            }
    }

    val dateOrderedWorkoutLogs by lazy {
        workoutLogs.sortedByDescending { it.date }
    }

    val weeklyCompletionChart by lazy {
        val workoutCount = program?.workouts?.size
        val completedWorkoutsByWeek = lastSevenWeeks.fastMap { week ->
            week.first to
                    workoutsInDateRange.filter { workoutLog ->
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

        val workoutsForCurrentMeso = workoutLogs
            .asSequence()
            .sortedByDescending { it.date }
            .groupBy { it.mesocycle }
            .values.firstOrNull()
            ?.map {
                it.historicalWorkoutNameId to it.setResults.size.div(setCount.toFloat()).times(100)
            }?.associate { (date, percentCompleted) ->
                date to percentCompleted
            } ?: mapOf()

        val xValues = List(workoutsForCurrentMeso.size) { it.toFloat() }.associateWith { workoutsForCurrentMeso.keys.toList()[it.roundToInt()] }
        val chartEntryModel = entryModelOf(xValues.keys.zip(workoutsForCurrentMeso.values, ::entryOf))

        ChartModel(
            chartEntryModel = chartEntryModel,
            axisValuesOverrider = object: AxisValuesOverrider<ChartEntryModel> {
                override fun getMinY(model: ChartEntryModel): Float {
                    return 0f
                }
                override fun getMaxY(model: ChartEntryModel): Float {
                    return 100f
                }
            },
            bottomAxisLabelRotationDegrees = 0f,
            bottomAxisValueFormatter = { value, _ ->
                xValues[value].toString()
            },
            startAxisValueFormatter = { value, _ ->
                "${value.roundToInt()}%"
            },
            startAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 11),
        )
    }
}
