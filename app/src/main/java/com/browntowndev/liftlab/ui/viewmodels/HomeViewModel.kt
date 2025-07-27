package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.authStateFlow
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
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.Lift
import com.browntowndev.liftlab.core.domain.models.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.Program
import com.browntowndev.liftlab.core.domain.models.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
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
import dev.gitlive.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import dev.gitlive.firebase.auth.android
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val workoutLogRepository: WorkoutLogRepository,
    private val liftMetricChartsRepository: LiftMetricChartsRepository,
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository,
    private val liftsRepository: LiftsRepository,
    private val onNavigateToSettingsMenu: () -> Unit,
    private val onNavigateToLiftLibrary: (chartIds: List<Long>) -> Unit,
    private val onUserLoggedIn: () -> Unit,
    private val firebaseAuth: FirebaseAuth,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        val dateRange = getSevenWeeksDateRange()
        val workoutCompletionRange = getLastSevenWeeksRange(dateRange)
        _state.update {
            it.copy(liftMetricOptions = getLiftMetricChartOptions())
        }

        viewModelScope.launch {
            val activeProgramFlow = programsRepository.getActiveProgramFlow()
            val liftsFlow = liftsRepository.getAllFlow()
            val workoutLogsFlow = workoutLogRepository.getAllFlow()
            val firebaseAuthStateFlow = firebaseAuth.authStateFlow()
            val liftMetricChartsFlow = liftMetricChartsRepository.getAllFlow()
            val volumeMetricChartsFlow = volumeMetricChartsRepository.getAllFlow()

            val programDataFlow = combine(
                activeProgramFlow,
                liftsFlow,
                workoutLogsFlow,
            ) { activeProgram, lifts, workoutLogs ->
                Triple(activeProgram, lifts, workoutLogs)
            }

            val chartsFlow = combine(
                liftMetricChartsFlow,
                volumeMetricChartsFlow,
            ) { liftMetricCharts, volumeMetricCharts ->
                Pair(liftMetricCharts, volumeMetricCharts)
            }

            combine(
                firebaseAuthStateFlow,
                programDataFlow,
                chartsFlow,
            ) { firebaseAuth, (activeProgram, lifts, workoutLogs), (liftMetricCharts, volumeMetricCharts) ->
                _state.value.copy(
                    firebaseUsername = firebaseAuth?.email,
                    emailVerified = firebaseAuth?.isEmailVerified ?: false,
                    activeProgram = activeProgram,
                    lifts = lifts,
                    workoutLogs = workoutLogs,
                    workoutCompletionChart = workoutLogs.let {
                        if (it.isEmpty()) null
                        else getWeeklyCompletionChart(
                            workoutCompletionRange = workoutCompletionRange,
                            workoutsInDateRange = getWorkoutsInDateRange(
                                it,
                                dateRange
                            )
                        )
                    },
                    microCycleCompletionChart = activeProgram?.let { program ->
                        if (workoutLogs.isEmpty()) null
                        else getMicroCycleCompletionChart(
                            workoutLogs = workoutLogs,
                            program = program,
                        )
                    },
                    volumeMetricChartModels = getVolumeMetricCharts( // this method handles empty lists itself
                        volumeMetricCharts = volumeMetricCharts,
                        workoutLogs = workoutLogs,
                        lifts = lifts,
                    ),
                    liftMetricChartModels = getLiftMetricCharts( // this method handles empty lists by itself
                        liftMetricCharts = liftMetricCharts,
                        workoutLogs = workoutLogs,
                    ),
                )
            }.collect {
                _state.value = it
            }
        }
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.OpenSettingsMenu -> onNavigateToSettingsMenu()
            TopAppBarAction.OpenProfileMenu -> toggleLoginModal()
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

    private fun getSevenWeeksDateRange(): Pair<Date, Date> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.minusWeeks(7).toStartOfDate() to today.toEndOfDate()
    }

    private fun getWorkoutsInDateRange(
        workoutLogs: List<WorkoutLogEntry>,
        dateRange: Pair<Date, Date>
    ): List<WorkoutLogEntry> {
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
        liftMetricCharts: List<LiftMetricChart>,
        workoutLogs: List<WorkoutLogEntry>,
    ): List<LiftMetricChartModel> {
        if (liftMetricCharts.isEmpty() || workoutLogs.isEmpty()) return emptyList()
        return liftMetricCharts.groupBy { it.liftId }.flatMap { liftCharts ->
            // Filter the workoutEntity logs to only include results for the current chart's liftEntity
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
            }.sortedBy { it.date }

            // Build all the selected charts for the liftEntity
            val liftName = resultsForLift.lastOrNull()?.setResults?.get(0)?.liftName
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
                }.fastMapNotNull { chartModel ->
                    if (chartModel.chartModel.chartEntryModel != null) chartModel else null
                }.sortedBy { it.liftName }
            } else listOf()
        }
    }

    private fun getVolumeMetricCharts(
        volumeMetricCharts: List<VolumeMetricChart>,
        workoutLogs: List<WorkoutLogEntry>,
        lifts: List<Lift>,
    ): List<VolumeMetricChartModel> {
        if (volumeMetricCharts.isEmpty() || workoutLogs.isEmpty() || lifts.isEmpty()) return emptyList()
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
        }.sortedBy { it.volumeType }
    }

    fun toggleLoginModal() {
        _state.update {
            it.copy(
                loginModalVisible = !state.value.loginModalVisible
            )
        }
    }

    fun createAccount(email: String, password: String) {
        viewModelScope.launch {
            try {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful) {
                            task.result?.user?.sendEmailVerification() ?: throw Exception("User is null after successful account creation.")
                        }
                        handleFirebaseTaskCompletion(task)
                    }
            } catch (ex: Exception) {
                handleFirebaseError(ex)
            }
        }
    }

    private fun handleFirebaseError(ex: Exception?) {
        _state.update {
            it.copy(firebaseError = "Failed to authenticate user.")
        }
        Log.e("Firebase", "Failed to authenticate user: ${ex ?: "Unknown error"}")
    }

    private fun handleFirebaseTaskCompletion(task: Task<AuthResult>) {
        if (task.isSuccessful) {
            val firebaseUser = task.result?.user

            if (firebaseUser != null) {
                _state.update {
                    it.copy(
                        firebaseUsername = firebaseUser.email,
                        emailVerified = firebaseUser.isEmailVerified,
                        firebaseError = null,
                    )
                }
                onUserLoggedIn()
                Log.d("Firebase", "User ${firebaseUser.email} successfully authenticated.")
            } else {
                // This case is highly unexpected if task.isSuccessful is true
                Log.e("Firebase", "User is null despite successful task.")
                _state.update {
                    it.copy(firebaseError = "Authentication successful, but user data is unavailable.")
                }
            }
        } else {
            handleFirebaseError(task.exception)
        }
    }


    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        handleFirebaseTaskCompletion(task)
                    }
            } catch (ex: Exception) {
                handleFirebaseError(ex)
            }
        }
    }

    fun logout() {
        Log.d("Firebase", "Logging out user ${firebaseAuth.currentUser?.uid}.")
        firebaseAuth.signOut()
        _state.update {
            it.copy(
                firebaseUsername = null,
                emailVerified = false,
            )
        }
    }

    fun signInWithGoogle(signInResult: Result<FirebaseUser?> ) {
        if (signInResult.isSuccess) {
            val firebaseUser: FirebaseUser? = signInResult.getOrNull()
            if (firebaseUser != null) {
                firebaseAuth.updateCurrentUser(firebaseUser.android)
                Log.d("Firebase", "User ${firebaseUser.email} successfully authenticated.")
                _state.update {
                    it.copy(
                        firebaseUsername = firebaseUser.displayName ?: firebaseUser.email,
                        emailVerified = firebaseUser.isEmailVerified,
                    )
                }
                onUserLoggedIn()
            } else {
                // Should be impossible. The library returns failure if user was null
                _state.update {
                    it.copy(
                        firebaseError = "Authentication successful, but user data is unavailable.",
                        firebaseUsername = null,
                        emailVerified = false,
                    )
                }
            }
        } else {
            Log.e("Firebase", "Failed to authenticate user: ${signInResult.exceptionOrNull() ?: "Unknown error"}")
            _state.update {
                it.copy(
                    firebaseError = "Failed to authenticate user.",
                    firebaseUsername = null,
                    emailVerified = false,
                )
            }
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
                VolumeMetricChart(
                    volumeType = volumeTypeStr.toVolumeType(),
                    volumeTypeImpact = _state.value.volumeImpactSelection?.toVolumeTypeImpact() ?: VolumeTypeImpact.COMBINED
                )
            }
            volumeMetricChartsRepository.upsertMany(charts)

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
                LiftMetricChart(
                    chartType = it.toLiftMetricChartType()
                )
            }
            // Clear out table of charts with no lifts in case any get stranded somehow
            liftMetricChartsRepository.deleteAllWithNoLifts()
            val chartIds = liftMetricChartsRepository.upsertMany(charts)
            onNavigateToLiftLibrary(chartIds)
        }
    }

    fun deleteLiftMetricChart(id: Long) {
        executeInTransactionScope {
            liftMetricChartsRepository.deleteById(id)
            _state.update {
                it.copy(
                    liftMetricChartModels = it.liftMetricChartModels.filter { chart -> chart.id != id }
                )
            }
        }
    }

    fun deleteVolumeMetricChart(id: Long) {
        executeInTransactionScope {
            volumeMetricChartsRepository.deleteById(id)
            _state.update {
                it.copy(
                    volumeMetricCharts = it.volumeMetricCharts.filter { chart -> chart.id != id },
                    volumeMetricChartModels = it.volumeMetricChartModels.filter { chart -> chart.id != id }
                )
            }
        }
    }
}