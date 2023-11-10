package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.toLiftMetricChartType
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.common.toStartOfDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftMetricChartRepository
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.ui.models.BaseChartModel
import com.browntowndev.liftlab.ui.models.ChartModel
import com.browntowndev.liftlab.ui.models.getIntensityChartModel
import com.browntowndev.liftlab.ui.models.getOneRepMaxChartModel
import com.browntowndev.liftlab.ui.models.getVolumeChartModel
import com.browntowndev.liftlab.ui.viewmodels.states.HomeState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.LiftLibraryScreen
import com.browntowndev.liftlab.ui.viewmodels.states.screens.SettingsScreen
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
import org.greenrobot.eventbus.Subscribe
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import kotlin.math.roundToInt

class HomeViewModel(
    private val programsRepository: ProgramsRepository,
    private val loggingRepository: LoggingRepository,
    private val liftMetricChartRepository: LiftMetricChartRepository,
    private val navHostController: NavHostController,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _programLiveData: LiveData<ProgramDto?>? = null
    private var _programObserver: Observer<ProgramDto?>? = null
    private var _loggingLiveData:  LiveData<List<WorkoutLogEntryDto>>? = null
    private var _loggingObserver: Observer<List<WorkoutLogEntryDto>>? = null
    private var _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val dateRange = getSevenWeeksDateRange()
            val workoutCompletionRange = getLastSevenWeeksRange(dateRange)
            val liftMetricCharts = liftMetricChartRepository.getAll()

            _programObserver = Observer { activeProgram ->
                if (_loggingLiveData == null) {
                    _loggingObserver = Observer { workoutLogs ->
                        val dateOrderedWorkoutLogs = workoutLogs.sortedByDescending { it.date }
                        val workoutsInDateRange = getWorkoutsInDateRange(dateOrderedWorkoutLogs, dateRange)

                        _state.update {
                            it.copy(
                                dateOrderedWorkoutLogs = dateOrderedWorkoutLogs,
                                liftMetricCharts = getLiftMetricCharts(
                                    liftMetricCharts = liftMetricCharts,
                                    workoutLogs = dateOrderedWorkoutLogs
                                ),
                                workoutCompletionChart = getWeeklyCompletionChart(
                                    workoutCompletionRange = workoutCompletionRange,
                                    workoutsInDateRange = workoutsInDateRange,
                                    program = activeProgram,
                                ),
                                microCycleCompletionChart = getMicroCycleCompletionChart(
                                    dateOrderedWorkoutLogs = dateOrderedWorkoutLogs,
                                    program = activeProgram,
                                )
                            )
                        }
                    }

                    _loggingLiveData = loggingRepository.getAll()
                    _loggingLiveData!!.observeForever(_loggingObserver!!)
                } else {
                    _state.update {
                        it.copy(
                            workoutCompletionChart = getWeeklyCompletionChart(
                                workoutCompletionRange = workoutCompletionRange,
                                workoutsInDateRange = getWorkoutsInDateRange(_state.value.dateOrderedWorkoutLogs, dateRange),
                                program = activeProgram,
                            ),
                            microCycleCompletionChart = getMicroCycleCompletionChart(
                                dateOrderedWorkoutLogs = _state.value.dateOrderedWorkoutLogs,
                                program = activeProgram,
                            )
                        )
                    }
                }
            }
            _programLiveData = programsRepository.getActive()
            _programLiveData!!.observeForever(_programObserver!!)
        }
    }

    override fun onCleared() {
        super.onCleared()

        _programLiveData?.removeObserver(_programObserver!!)
        _loggingLiveData?.removeObserver(_loggingObserver!!)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.OpenSettingsMenu -> navHostController.navigate(SettingsScreen.navigation.route)
            else -> { }
        }
    }

    private fun getSevenWeeksDateRange(): Pair<Date, Date> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.minusWeeks(7).toStartOfDate() to today.toEndOfDate()
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
        workoutsInDateRange: List<WorkoutLogEntryDto>,
        program: ProgramDto?,
    ): ChartModel {
        val workoutCount = program?.workouts?.size
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

    private fun getMicroCycleCompletionChart(
        dateOrderedWorkoutLogs: List<WorkoutLogEntryDto>,
        program: ProgramDto?,
    ): ChartModel {
        val setCount = program?.workouts?.sumOf { workout ->
            workout.lifts.sumOf { it.setCount }
        }?.toFloat() ?: 1f

        val workoutsForCurrentMeso = dateOrderedWorkoutLogs
            .groupBy { it.mesocycle }
            .values.firstOrNull()
            ?.groupBy { it.microcycle }
            ?.asSequence()
            ?.associate { logsForMicro ->
                logsForMicro.key + 1 to logsForMicro.value.sumOf {  workoutLog ->
                    workoutLog.setResults.size
                }.div(setCount).times(100)
            } ?: mapOf(1 to 0f)

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

    private fun getLiftMetricCharts(
        liftMetricCharts: List<LiftMetricChartDto>,
        workoutLogs: List<WorkoutLogEntryDto>
    ): List<Pair<String, List<BaseChartModel>>> {
        return liftMetricCharts.groupBy { it.liftId }.mapNotNull { liftCharts ->

            // Filter the workout logs to only include results for the current chart's lift
            val resultsForLift = workoutLogs.mapNotNull { workoutLog ->
                workoutLog.setResults
                    .filter { it.liftId == liftCharts.key }
                    .let { filteredResults ->
                        if (filteredResults.isNotEmpty()) {
                            workoutLog.copy(
                                setResults = filteredResults
                            )
                        } else {
                            null
                        }
                    }
            }

            // Build all the selected charts for the lift
            val liftName = resultsForLift.firstOrNull()?.setResults?.get(0)?.liftName
            if (liftName != null) {
                val chartModels = liftCharts.value.fastMap { chart ->
                    when (chart.chartType) {
                        LiftMetricChartType.ESTIMATED_ONE_REP_MAX -> getOneRepMaxChartModel(resultsForLift, setOf())
                        LiftMetricChartType.VOLUME -> getVolumeChartModel(resultsForLift, setOf())
                        LiftMetricChartType.RELATIVE_INTENSITY -> getIntensityChartModel(resultsForLift, setOf())
                    }
                }
                liftName to chartModels // Use the infix function to create a pair
            } else {
                null // Return null if there are no results for the lift
            }
        }
    }

    fun toggleLiftChartPicker() {
        _state.update {
            it.copy(showLiftChartPicker = !it.showLiftChartPicker)
        }
    }

    fun updateLiftChartTypeSelections(type: String, selected: Boolean) {
        _state.update {
            it.copy(
                liftChartTypeSelections = it.liftChartTypeSelections.toMutableList().apply {
                    if (selected) {
                        add(type)
                    } else {
                        remove(type)
                    }
                }
            )
        }
    }

    fun selectLiftForMetricCharts() {
        executeInTransactionScope {
            val charts = _state.value.liftChartTypeSelections.fastMap {
                LiftMetricChartDto(
                    chartType = it.toLiftMetricChartType()
                )
            }
            // Clear out table of charts with no lifts in case any get stranded somehow
            liftMetricChartRepository.deleteAllWithNoLifts()
            liftMetricChartRepository.upsertMany(charts)
            navHostController.navigate(LiftLibraryScreen.navigation.route)
        }
    }
}