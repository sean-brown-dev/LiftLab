package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.common.toStartOfDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.viewmodels.states.HomeScreenState
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import kotlin.math.roundToInt

class HomeScreenViewModel(
    private val programsRepository: ProgramsRepository,
    private val loggingRepository: LoggingRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val dateRange = getSevenWeeksDateRange()
            val workoutCompletionRange = getLastSevenWeeksRange(dateRange)

            programsRepository.getActive().observeForever { activeProgram ->
                _state.update {
                    it.copy(
                        program = activeProgram
                    )
                }

                loggingRepository.getAll().observeForever { workoutLogs ->
                    val dateOrderedWorkoutLogs = workoutLogs.sortedByDescending { it.date }
                    val workoutsInDateRange = getWorkoutsInDateRange(dateOrderedWorkoutLogs, dateRange)

                    _state.update {
                        it.copy(
                            dateOrderedWorkoutLogs = dateOrderedWorkoutLogs,
                            topSets = getTopSets(dateOrderedWorkoutLogs),
                            workoutCompletionChart = getWeeklyCompletionChart(
                                workoutCompletionRange = workoutCompletionRange,
                                workoutsInDateRange = workoutsInDateRange,
                            ),
                            microCycleCompletionChart = getMicroCycleCompletionChart(
                                dateOrderedWorkoutLogs = dateOrderedWorkoutLogs
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getSevenWeeksDateRange(): Pair<Date, Date> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.minusWeeks(7).toStartOfDate() to today.toEndOfDate()
    }

    private fun getTopSets(workoutLogs: List<WorkoutLogEntryDto>): Map<String, Pair<Int, SetLogEntryDto>> {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults
                .groupBy { set ->
                    set.liftName
                }.map { setsForLift ->
                    setsForLift.key to (setsForLift.value.size to setsForLift.value.maxBy {
                        CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                    })
                }
        }.associate { topSet -> topSet.first to topSet.second }
    }

    private fun getWorkoutsInDateRange(
        dateOrderedWorkoutLogs: List<WorkoutLogEntryDto>,
        dateRange: Pair<Date, Date>
    ): List<WorkoutLogEntryDto> {
        return dateOrderedWorkoutLogs
            .filter { workoutLog ->
                dateRange.first <= workoutLog.date &&
                        workoutLog.date <= dateRange.second
            }
    }

    private fun getLastSevenWeeksRange(dateRange: Pair<Date, Date>): List<Pair<LocalDate, LocalDate>> {
        return (0..7).map { i ->
            val monday = dateRange.first.toLocalDate().plusDays(i * 7L)
            val sunday = monday.plusDays(6L)
            monday to sunday
        }
    }

    private fun getWeeklyCompletionChart(
        workoutCompletionRange: List<Pair<LocalDate, LocalDate>>,
        workoutsInDateRange: List<WorkoutLogEntryDto>
    ): ChartModel {
        val workoutCount = _state.value.program?.workouts?.size
        val completedWorkoutsByWeek = workoutCompletionRange.fastMap { week ->
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

        return ChartModel(
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

    private fun getMicroCycleCompletionChart(dateOrderedWorkoutLogs: List<WorkoutLogEntryDto>): ChartModel {
        val setCount = _state.value.program?.workouts?.sumOf { workout ->
            workout.lifts.sumOf { it.setCount }
        }?.toFloat() ?: 1f

        val workoutsForCurrentMeso = dateOrderedWorkoutLogs
            .asSequence()
            .sortedByDescending { it.date }
            .groupBy { it.mesocycle }
            .values.firstOrNull()
            ?.groupBy { it.microcycle }
            ?.asSequence()
            ?.associate { logsForMicro ->
                logsForMicro.key + 1 to logsForMicro.value.sumOf {  workoutLog ->
                    workoutLog.setResults.size
                }.div(setCount).times(100)
            } ?: mapOf()

        val chartEntryModel = entryModelOf(workoutsForCurrentMeso.keys.zip(workoutsForCurrentMeso.values, ::entryOf))

        return ChartModel(
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
                value.roundToInt().toString()
            },
            startAxisValueFormatter = { value, _ ->
                "${value.roundToInt()}%"
            },
            startAxisItemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 11),
        )
    }
}