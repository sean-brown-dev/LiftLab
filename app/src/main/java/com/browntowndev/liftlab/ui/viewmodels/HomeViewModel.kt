package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getSevenWeeksDateRange
import com.browntowndev.liftlab.core.common.authStateFlow
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.enums.toLiftMetricChartType
import com.browntowndev.liftlab.core.domain.enums.toVolumeType
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypeImpact
import com.browntowndev.liftlab.ui.models.TopAppBarEvent
import com.browntowndev.liftlab.core.common.getLastSevenWeeksInRange
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.extensions.filterByDateRange
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.useCase.charts.GetGroupedLiftMetricChartDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.charts.GetGroupedVolumeMetricChartDataUseCase
import com.browntowndev.liftlab.ui.factory.LiftMetricChartOptionActions
import com.browntowndev.liftlab.ui.factory.createLiftMetricChartOptions
import com.browntowndev.liftlab.ui.mapping.ChartMappingExtensions.toChartModels
import com.browntowndev.liftlab.ui.mapping.ChartMappingExtensions.toVolumeMetricChartModels
import com.browntowndev.liftlab.ui.models.getMicroCycleCompletionChart
import com.browntowndev.liftlab.ui.models.getWeeklyCompletionChart
import com.browntowndev.liftlab.ui.viewmodels.states.HomeState
import dev.gitlive.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class HomeViewModel(
    programsRepository: ProgramsRepository,
    workoutLogRepository: WorkoutLogRepository,
    liftsRepository: LiftsRepository,
    private val liftMetricChartsRepository: LiftMetricChartsRepository,
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository,
    private val getGroupedLiftMetricChartDataUseCase: GetGroupedLiftMetricChartDataUseCase,
    private val getGroupedVolumeMetricChartDataUseCase: GetGroupedVolumeMetricChartDataUseCase,
    private val onNavigateToSettingsMenu: () -> Unit,
    private val onNavigateToLiftLibrary: (chartIds: List<Long>) -> Unit,
    private val onUserLoggedIn: () -> Unit,
    private val firebaseAuth: FirebaseAuth,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    companion object {
        private const val TAG = "HomeViewModel"
    }

    private var _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        val dateRange = getSevenWeeksDateRange()
        val workoutCompletionRange = dateRange.getLastSevenWeeksInRange()

        val chartPickerActions = LiftMetricChartOptionActions(
            onSelectLiftForMetricCharts = ::selectLiftForMetricCharts,
            onUpdateLiftChartTypeSelections = ::updateLiftChartTypeSelections,
            onAddVolumeMetricChart = ::addVolumeMetricChart,
            onUpdateVolumeTypeImpactSelection = ::updateVolumeTypeImpactSelection,
            onUpdateVolumeTypeSelections = ::updateVolumeTypeSelections
        )
        _state.update {
            it.copy(liftMetricOptions =
                createLiftMetricChartOptions(
                    actions = chartPickerActions
                )
            )
        }

        // 1. Source Data Flows
        val activeProgramFlow = programsRepository.getActiveProgramFlow()
        val liftsFlow = liftsRepository.getAllFlow()
        val workoutLogsFlow = workoutLogRepository.getAllFlow()
        val firebaseAuthFlow = firebaseAuth.authStateFlow()
        val liftMetricChartsFlow = liftMetricChartsRepository.getAllFlow()
        val volumeMetricChartsFlow = volumeMetricChartsRepository.getAllFlow()

        // 2. Derived Chart Model Flows
        // These only recompute when a dependency changes
        val workoutCompletionChartFlow = workoutLogsFlow.map { logs ->
            if (logs.isEmpty()) null
            else getWeeklyCompletionChart(
                workoutCompletionRange = workoutCompletionRange,
                workoutsInDateRange = logs.filterByDateRange(dateRange)
            )
        }

        val microCycleCompletionChartFlow = combine(activeProgramFlow, workoutLogsFlow) { program, logs ->
            program?.let {
                if (logs.isEmpty()) null
                else getMicroCycleCompletionChart(workoutLogs = logs, program = it)
            }
        }

        val volumeMetricChartModelsFlow = combine(volumeMetricChartsFlow, workoutLogsFlow, liftsFlow) { charts, logs, allLifts ->
            allLifts.toVolumeMetricChartModels(
                groupedData = getGroupedVolumeMetricChartDataUseCase(
                    volumeMetricCharts = charts,
                    workoutLogs = logs,
                    lifts = allLifts,
                )
            )
        }

        val liftMetricChartModelsFlow = combine(liftMetricChartsFlow, workoutLogsFlow) { charts, logs ->
            charts.toChartModels(
                groupedLogs = getGroupedLiftMetricChartDataUseCase(
                    liftMetricCharts = charts,
                    workoutLogs = logs,
                )
            )
        }

        // 3. Combine all flows to build the final UI State
        // We nest combines because the standard function only supports up to 5 arguments.
        // This remains type-safe and efficient.
        val combinedSourceDataFlow = combine(
            firebaseAuthFlow,
            activeProgramFlow,
            liftsFlow,
            workoutLogsFlow,
            liftMetricChartsFlow
        ) { auth, program, lifts, logs, liftCharts ->
            object {
                val auth = auth
                val program = program
                val lifts = lifts
                val logs = logs
                val liftCharts = liftCharts
            }
        }.combine(volumeMetricChartsFlow) { allCombinedFlows, volumeCharts ->
            object {
                val allSourceData = allCombinedFlows
                val volumeCharts = volumeCharts
            }
        }

        val combinedDerivedChartsFlow = combine(
            workoutCompletionChartFlow,
            microCycleCompletionChartFlow,
            volumeMetricChartModelsFlow,
            liftMetricChartModelsFlow
        ) { wcChart, mcChart, vmModels, lmModels ->
            object {
                val workoutCompletionChart = wcChart
                val microCycleCompletionChart = mcChart
                val volumeMetricChartModels = vmModels
                val liftMetricChartModels = lmModels
            }
        }

        combine(combinedSourceDataFlow, combinedDerivedChartsFlow) { sourceData, charts ->
            _state.update {
                it.copy(
                    firebaseUsername = sourceData.allSourceData.auth?.email,
                    emailVerified = sourceData.allSourceData.auth?.isEmailVerified ?: false,
                    firebaseError = null,
                    activeProgram = sourceData.allSourceData.program,
                    lifts = sourceData.allSourceData.lifts,
                    workoutLogs = sourceData.allSourceData.logs,
                    liftMetricCharts = sourceData.allSourceData.liftCharts,
                    volumeMetricCharts = sourceData.volumeCharts,
                    workoutCompletionChart = charts.workoutCompletionChart,
                    microCycleCompletionChart = charts.microCycleCompletionChart,
                    volumeMetricChartModels = charts.volumeMetricChartModels,
                    liftMetricChartModels = charts.liftMetricChartModels
                )
            }
        }.catch { exception ->
            Log.e(TAG, "Error combining flows: $exception", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
            emitUserMessage("Failed to load Home")
        }.launchIn(viewModelScope)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.OpenSettingsMenu -> onNavigateToSettingsMenu()
            TopAppBarAction.OpenProfileMenu -> toggleLoginModal()
            else -> { }
        }
    }

    fun toggleLoginModal() {
        _state.update {
            it.copy(
                loginModalVisible = !state.value.loginModalVisible
            )
        }
    }

    fun createAccount(email: String, password: String) = executeWithErrorHandling("Failed to create account.") {
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


    fun login(email: String, password: String) = executeWithErrorHandling("Failed to log in user.") {
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

    fun logout() = executeWithErrorHandling("Failed to log out user.") {
        Log.d("Firebase", "Logging out user ${firebaseAuth.currentUser?.uid}.")
        firebaseAuth.signOut()
    }

    fun signInWithGoogle(signInResult: Result<FirebaseUser?> ) = executeWithErrorHandling("Failed to authenticate user.") {
        if (signInResult.isSuccess) {
            val firebaseUser: FirebaseUser? = signInResult.getOrNull()
            if (firebaseUser != null) {
                Log.d("Firebase", "User ${firebaseUser.email} successfully authenticated.")
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

    private fun addVolumeMetricChart() = executeWithErrorHandling("Failed to add volume metric chart") {
        executeInTransactionScope {
            val charts = _state.value.volumeTypeSelections.fastMap { volumeTypeStr ->
                VolumeMetricChart(
                    volumeType = volumeTypeStr.toVolumeType(),
                    volumeTypeImpact = _state.value.volumeImpactSelection?.toVolumeTypeImpact() ?: VolumeTypeImpact.COMBINED
                )
            }
            volumeMetricChartsRepository.upsertMany(charts)
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

    private fun selectLiftForMetricCharts() = executeWithErrorHandling("Failed to select lift for metric charts") {
        viewModelScope.launch {
            val charts = _state.value.liftChartTypeSelections.fastMap {
                LiftMetricChart(
                    chartType = it.toLiftMetricChartType()
                )
            }
            // Clear out table of charts with no lifts in case any get stranded somehow
            liftMetricChartsRepository.deleteAllWithNoLifts()
            val chartIds = liftMetricChartsRepository.insertMany(charts)
            onNavigateToLiftLibrary(chartIds)
        }
    }

    fun deleteLiftMetricChart(id: Long) = executeWithErrorHandling("Failed to delete lift metric chart") {
        executeInTransactionScope {
            liftMetricChartsRepository.deleteById(id)
        }
    }

    fun deleteVolumeMetricChart(id: Long) = executeWithErrorHandling("Failed to delete volume metric chart") {
        executeInTransactionScope {
            volumeMetricChartsRepository.deleteById(id)
        }
    }
}