package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.getVolumeTypes
import com.browntowndev.liftlab.core.common.enums.toLiftMetricChartType
import com.browntowndev.liftlab.core.common.enums.toVolumeType
import com.browntowndev.liftlab.core.common.enums.toVolumeTypeImpact
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toEndOfDate
import com.browntowndev.liftlab.core.common.toLocalDate
import com.browntowndev.liftlab.core.common.toStartOfDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.VolumeMetricChartDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftMetricChartRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.VolumeMetricChartRepository
import com.browntowndev.liftlab.ui.models.LiftMetricChartModel
import com.browntowndev.liftlab.ui.models.LiftMetricOptionTree
import com.browntowndev.liftlab.ui.models.LiftMetricOptions
import com.browntowndev.liftlab.ui.models.VolumeMetricChartModel
import com.browntowndev.liftlab.ui.models.getIntensityChartModel
import com.browntowndev.liftlab.ui.models.getMicroCycleCompletionChart
import com.browntowndev.liftlab.ui.models.getOneRepMaxChartModel
import com.browntowndev.liftlab.ui.models.getPerMicrocycleVolumeChartModel
import com.browntowndev.liftlab.ui.models.getPerWorkoutVolumeChartModel
import com.browntowndev.liftlab.ui.models.getWeeklyCompletionChart
import com.browntowndev.liftlab.ui.viewmodels.states.HomeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Date

class HomeViewModel(
    private val programsRepository: ProgramsRepository,
    private val loggingRepository: LoggingRepository,
    private val liftMetricChartRepository: LiftMetricChartRepository,
    private val volumeMetricChartRepository: VolumeMetricChartRepository,
    private val liftsRepository: LiftsRepository,
    private val onNavigateToSettingsMenu: () -> Unit,
    private val onNavigateToLiftLibrary: (chartIds: List<Long>) -> Unit,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _programLiveData: LiveData<ProgramDto?>? = null
    private var _programObserver: Observer<ProgramDto?>? = null
    private var _loggingLiveData:  LiveData<List<WorkoutLogEntryDto>>? = null
    private var _loggingObserver: Observer<List<WorkoutLogEntryDto>>? = null
    private var _liftLiveData: LiveData<List<LiftDto>>? = null
    private var _liftObserver: Observer<List<LiftDto>>? = null
    private var _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    liftMetricOptions = getLiftMetricChartOptions()
                )
            }

            val dateRange = getSevenWeeksDateRange()
            val workoutCompletionRange = getLastSevenWeeksRange(dateRange)
            val liftMetricCharts = liftMetricChartRepository.getAll()
            val volumeMetricCharts = volumeMetricChartRepository.getAll()
                .sortedWith(compareBy<VolumeMetricChartDto> { it.volumeType.bitMask }
                    .thenBy { it.volumeTypeImpact.bitmask }
                )

            _programObserver = Observer { activeProgram ->
                _state.update {
                    it.copy(activeProgramName = activeProgram?.name ?: "")
                }

                if (_liftLiveData == null) {
                    _liftObserver = Observer { lifts ->
                        _state.update {
                            it.copy(
                                lifts = lifts,
                                volumeMetricCharts = volumeMetricCharts,
                            )
                        }

                        if (_loggingLiveData == null) {
                            _loggingObserver = Observer { workoutLogs ->
                                onWorkoutLogsChanged(
                                    workoutLogs = workoutLogs,
                                    dateRange = dateRange,
                                    volumeMetricCharts = volumeMetricCharts,
                                    liftMetricCharts = liftMetricCharts,
                                    workoutCompletionRange = workoutCompletionRange,
                                    activeProgram = activeProgram
                                )
                            }

                            _loggingLiveData = loggingRepository.getAll()
                            _loggingLiveData!!.observeForever(_loggingObserver!!)
                        } else {
                            onActiveProgramOrLiftsChanged(
                                workoutCompletionRange = workoutCompletionRange,
                                dateRange = dateRange,
                                activeProgram = activeProgram,
                                volumeMetricCharts = volumeMetricCharts
                            )
                        }
                    }

                    _liftLiveData = liftsRepository.getAllAsLiveData()
                    _liftLiveData!!.observeForever(_liftObserver!!)
                } else {
                    onActiveProgramOrLiftsChanged(
                        workoutCompletionRange = workoutCompletionRange,
                        dateRange = dateRange,
                        activeProgram = activeProgram,
                        volumeMetricCharts = volumeMetricCharts
                    )
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
            TopAppBarAction.OpenSettingsMenu -> onNavigateToSettingsMenu()
            else -> { }
        }
    }

    private fun getLiftMetricChartOptions(): LiftMetricOptionTree {
        return LiftMetricOptionTree(
            completionButtonText = "Next",
            completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            options = listOf(
                LiftMetricOptions(
                    options = listOf("Lift Metrics"),
                    child = LiftMetricOptions(
                        options = LiftMetricChartType.entries.map { chartType -> chartType.displayName() },
                        completionButtonText = "Choose Lift",
                        completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        onCompletion = { selectLiftForMetricCharts() },
                        onSelectionChanged = { type, selected ->
                            updateLiftChartTypeSelections(
                                type,
                                selected
                            )
                        }
                    ),
                    completionButtonText = "Next",
                    completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                ),
                LiftMetricOptions(
                    options = listOf("Volume Metrics"),
                    child = LiftMetricOptions(
                        options = VolumeType.entries.map { volumeType ->
                            volumeType.displayName()
                        },
                        child = LiftMetricOptions(
                            options = VolumeTypeImpact.entries.map { volumeTypeImpact -> volumeTypeImpact.displayName() },
                            completionButtonText = "Confirm",
                            completionButtonIcon = Icons.Filled.Check,
                            onCompletion = { addVolumeMetricChart() },
                            onSelectionChanged = { type, selected ->
                                updateVolumeTypeImpactSelection(
                                    type,
                                    selected
                                )
                            },
                        ),
                        completionButtonText = "Next",
                        completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        onSelectionChanged = { type, selected ->
                            updateVolumeTypeSelections(
                                type,
                                selected
                            )
                        },
                    ),
                    completionButtonText = "Next",
                    completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                ),
            )
        )
    }

    private fun onWorkoutLogsChanged(
        workoutLogs: List<WorkoutLogEntryDto>,
        dateRange: Pair<Date, Date>,
        volumeMetricCharts: List<VolumeMetricChartDto>,
        liftMetricCharts: List<LiftMetricChartDto>,
        workoutCompletionRange: List<Pair<LocalDate, LocalDate>>,
        activeProgram: ProgramDto?
    ) {
        val workoutsInDateRange = getWorkoutsInDateRange(workoutLogs, dateRange)
        _state.update {
            it.copy(
                workoutLogs = workoutLogs,
                volumeMetricChartModels = getVolumeMetricCharts(
                    volumeMetricCharts = volumeMetricCharts,
                    workoutLogs = workoutLogs,
                    lifts = _state.value.lifts,
                ),
                liftMetricChartModels = getLiftMetricCharts(
                    liftMetricCharts = liftMetricCharts,
                    workoutLogs = workoutLogs,
                ),
                workoutCompletionChart = getWeeklyCompletionChart(
                    workoutCompletionRange = workoutCompletionRange,
                    workoutsInDateRange = workoutsInDateRange,
                ),
                microCycleCompletionChart = getMicroCycleCompletionChart(
                    workoutLogs = workoutLogs,
                    program = activeProgram,
                )
            )
        }
    }

    private fun onActiveProgramOrLiftsChanged(
        workoutCompletionRange: List<Pair<LocalDate, LocalDate>>,
        dateRange: Pair<Date, Date>,
        activeProgram: ProgramDto?,
        volumeMetricCharts: List<VolumeMetricChartDto>
    ) {
        _state.update {
            it.copy(
                workoutCompletionChart = getWeeklyCompletionChart(
                    workoutCompletionRange = workoutCompletionRange,
                    workoutsInDateRange = getWorkoutsInDateRange(
                        _state.value.workoutLogs,
                        dateRange
                    ),
                ),
                microCycleCompletionChart = getMicroCycleCompletionChart(
                    workoutLogs = _state.value.workoutLogs,
                    program = activeProgram,
                ),
                volumeMetricChartModels = getVolumeMetricCharts(
                    volumeMetricCharts = volumeMetricCharts,
                    workoutLogs = _state.value.workoutLogs,
                    lifts = _state.value.lifts,
                ),
            )
        }
    }

    private fun getSevenWeeksDateRange(): Pair<Date, Date> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.minusWeeks(7).toStartOfDate() to today.toEndOfDate()
    }

    private fun getWorkoutsInDateRange(
        workoutLogs: List<WorkoutLogEntryDto>,
        dateRange: Pair<Date, Date>
    ): List<WorkoutLogEntryDto> {
        return workoutLogs
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

    private fun getLiftMetricCharts(
        liftMetricCharts: List<LiftMetricChartDto>,
        workoutLogs: List<WorkoutLogEntryDto>,
    ): List<LiftMetricChartModel> {
        return liftMetricCharts.groupBy { it.liftId }.flatMap { liftCharts ->
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
                liftCharts.value.fastMap { chart ->
                    LiftMetricChartModel(
                        id = chart.id,
                        liftName = liftName,
                        type = chart.chartType,
                        chartModel = when (chart.chartType) {
                            LiftMetricChartType.ESTIMATED_ONE_REP_MAX -> getOneRepMaxChartModel(
                                resultsForLift,
                                setOf()
                            )

                            LiftMetricChartType.VOLUME -> getPerWorkoutVolumeChartModel(
                                resultsForLift,
                                setOf()
                            )

                            LiftMetricChartType.RELATIVE_INTENSITY -> getIntensityChartModel(
                                resultsForLift,
                                setOf()
                            )
                        }
                    )
                }
            } else listOf()
        }
    }

    private fun getVolumeMetricCharts(
        volumeMetricCharts: List<VolumeMetricChartDto>,
        workoutLogs: List<WorkoutLogEntryDto>,
        lifts: List<LiftDto>,
    ): List<VolumeMetricChartModel> {
        val primaryVolumeTypesById = lifts.associate { it.id to it.volumeTypesBitmask }
        val secondaryVolumeTypesById = lifts.associate { it.id to it.secondaryVolumeTypesBitmask }

        return volumeMetricCharts.mapNotNull { volumeChart ->
            val workoutLogsForChart = workoutLogs.mapNotNull { workoutLog ->
                workoutLog.setResults.filter { setLog ->
                    val primaryVolumeTypes = primaryVolumeTypesById[setLog.liftId]?.getVolumeTypes()?.toHashSet()
                    val secondaryVolumeTypes = secondaryVolumeTypesById[setLog.liftId]?.getVolumeTypes()?.toHashSet()

                    when (volumeChart.volumeTypeImpact) {
                        VolumeTypeImpact.COMBINED -> {
                            primaryVolumeTypes?.contains(volumeChart.volumeType) == true ||
                                    secondaryVolumeTypes?.contains(volumeChart.volumeType) == true
                        }
                        VolumeTypeImpact.PRIMARY -> primaryVolumeTypes?.contains(volumeChart.volumeType) == true
                        VolumeTypeImpact.SECONDARY -> secondaryVolumeTypes?.contains(volumeChart.volumeType) == true
                    }
                }.let { filteredSetLogs ->
                    if (filteredSetLogs.any()) {
                        workoutLog.copy(setResults = filteredSetLogs)
                    } else {
                        null
                    }
                }
            }

            if (workoutLogsForChart.isNotEmpty()) {
                VolumeMetricChartModel(
                    id = volumeChart.id,
                    volumeType = volumeChart.volumeType.displayName(),
                    volumeTypeImpact = volumeChart.volumeTypeImpact.displayName(),
                    chartModel = getPerMicrocycleVolumeChartModel(
                        workoutLogs = workoutLogsForChart,
                        secondaryVolumeTypesByLiftId = if (volumeChart.volumeTypeImpact != VolumeTypeImpact.PRIMARY)
                            secondaryVolumeTypesById else null,
                    )
                )
            } else null
        }
    }

    fun toggleLiftChartPicker() {
        _state.update {
            it.copy(
                showLiftChartPicker = !it.showLiftChartPicker,
                volumeTypeSelections = listOf(),
                volumeImpactSelection = null,
                liftChartTypeSelections = listOf(),
            )
        }
    }

    private fun updateVolumeTypeSelections(type: String, selected: Boolean) {
        _state.update {
            it.copy(
                volumeTypeSelections = it.volumeTypeSelections.toMutableList().apply {
                    if (selected) {
                        add(type)
                    } else {
                        remove(type)
                    }
                }
            )
        }
    }

    private fun updateVolumeTypeImpactSelection(type: String, selected: Boolean) {
        _state.update {
            it.copy(
                volumeImpactSelection = if (selected) {
                    type
                } else {
                    null
                }
            )
        }
    }

    private fun addVolumeMetricChart() {
        executeInTransactionScope {
            val charts = _state.value.volumeTypeSelections.fastMap { volumeTypeStr ->
                VolumeMetricChartDto(
                    volumeType = volumeTypeStr.toVolumeType(),
                    volumeTypeImpact = _state.value.volumeImpactSelection?.toVolumeTypeImpact() ?: VolumeTypeImpact.COMBINED
                )
            }
            volumeMetricChartRepository.upsertMany(charts)

            val chartsWithNewAdded = _state.value.volumeMetricCharts.toMutableList().apply {
                addAll(charts)
            }
            _state.update {
                it.copy(
                    volumeMetricCharts = chartsWithNewAdded,
                    volumeMetricChartModels = getVolumeMetricCharts(
                        volumeMetricCharts = chartsWithNewAdded,
                        workoutLogs = _state.value.workoutLogs,
                        lifts = _state.value.lifts,
                    )
                )
            }
            toggleLiftChartPicker()
        }
    }

    private fun updateLiftChartTypeSelections(type: String, selected: Boolean) {
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

    private fun selectLiftForMetricCharts() {
        viewModelScope.launch {
            val charts = _state.value.liftChartTypeSelections.fastMap {
                LiftMetricChartDto(
                    chartType = it.toLiftMetricChartType()
                )
            }
            // Clear out table of charts with no lifts in case any get stranded somehow
            liftMetricChartRepository.deleteAllWithNoLifts()
            val chartIds = liftMetricChartRepository.upsertMany(charts)
            onNavigateToLiftLibrary(chartIds)
        }
    }

    fun deleteLiftMetricChart(id: Long) {
        executeInTransactionScope {
            liftMetricChartRepository.delete(id)
            _state.update {
                it.copy(
                    liftMetricChartModels = it.liftMetricChartModels.filter { chart -> chart.id != id }
                )
            }
        }
    }

    fun deleteVolumeMetricChart(id: Long) {
        executeInTransactionScope {
            volumeMetricChartRepository.delete(id)
            _state.update {
                it.copy(
                    volumeMetricCharts = it.volumeMetricCharts.filter { chart -> chart.id != id },
                    volumeMetricChartModels = it.volumeMetricChartModels.filter { chart -> chart.id != id }
                )
            }
        }
    }
}