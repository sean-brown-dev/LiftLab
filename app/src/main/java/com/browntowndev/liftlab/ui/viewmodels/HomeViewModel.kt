package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getSevenWeeksDateRange
import com.browntowndev.liftlab.core.common.authStateFlow
import com.browntowndev.liftlab.core.common.getLastSevenWeeksInRange
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection
import com.browntowndev.liftlab.core.domain.enums.toLiftMetricChartType
import com.browntowndev.liftlab.core.domain.models.metrics.ConfiguredMetricsState
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.useCase.metrics.DeleteLiftMetricChartByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.DeleteVolumeMetricChartByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetConfiguredMetricsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.InsertManyLiftMetricChartsUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.UpsertManyVolumeMetricChartsUseCase
import com.browntowndev.liftlab.ui.factory.LiftMetricChartOptionActions
import com.browntowndev.liftlab.ui.factory.createLiftMetricChartOptions
import com.browntowndev.liftlab.ui.mapping.ChartMappingExtensions.toChartModels
import com.browntowndev.liftlab.ui.mapping.ChartMappingExtensions.toVolumeMetricChartModels
import com.browntowndev.liftlab.ui.mapping.ProgramMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutHistoryMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.metrics.LiftMetricOptionTree
import com.browntowndev.liftlab.ui.models.metrics.getMicroCycleCompletionChart
import com.browntowndev.liftlab.ui.models.metrics.getWeeklyCompletionChart
import com.browntowndev.liftlab.ui.models.workout.toVolumeTypeImpact
import com.browntowndev.liftlab.ui.models.workoutLogging.filterByDateRange
import com.browntowndev.liftlab.ui.viewmodels.states.HomeState
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class HomeViewModel(
    getConfiguredMetricsStateFlowUseCase: GetConfiguredMetricsStateFlowUseCase,
    private val upsertManyVolumeMetricChartsUseCase: UpsertManyVolumeMetricChartsUseCase,
    private val deleteVolumeMetricChartByIdUseCase: DeleteVolumeMetricChartByIdUseCase,
    private val deleteLiftMetricChartByIdUseCase: DeleteLiftMetricChartByIdUseCase,
    private val insertManyLiftMetricChartsUseCase: InsertManyLiftMetricChartsUseCase,
    private val onNavigateToSettingsMenu: () -> Unit,
    private val onNavigateToLiftLibrary: (chartIds: List<Long>) -> Unit,
    private val onUserLoggedIn: () -> Unit,
    private val firebaseAuth: FirebaseAuth,
    eventBus: EventBus,
): BaseViewModel(eventBus) {
    companion object {
        private const val TAG = "HomeViewModel"
    }

    private var _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        val dateRange = getSevenWeeksDateRange()
        val workoutCompletionRange = dateRange.getLastSevenWeeksInRange()

        val firebaseAuthFlow = firebaseAuth.authStateFlow().distinctUntilChanged()
        getConfiguredMetricsStateFlowUseCase()
            .distinctUntilChanged()
            .scan(ConfiguredMetricsState() to HomeState()) { currentStates, newConfiguredMetricsState ->
                val currentConfiguredMetricsState = currentStates.first
                val currentHomeState = currentStates.second

                var newHomeState = HomeState(
                    activeProgram = newConfiguredMetricsState.activeProgram?.toUiModel(),
                    lifts = newConfiguredMetricsState.lifts,
                    workoutLogs = newConfiguredMetricsState.workoutLogs.fastMap { it.toUiModel() },
                )

                newHomeState = if (currentHomeState.activeProgram != newHomeState.activeProgram ||
                    currentHomeState.workoutLogs != newHomeState.workoutLogs) {
                    newHomeState.copy(
                        microCycleCompletionChart = if (newHomeState.workoutLogs.isEmpty()) null
                            else getMicroCycleCompletionChart(workoutLogs = newHomeState.workoutLogs, program = newHomeState.activeProgram)
                    )
                } else newHomeState.copy(
                    microCycleCompletionChart = currentHomeState.microCycleCompletionChart
                )

                newHomeState = if (currentHomeState.workoutLogs != newHomeState.workoutLogs) {
                    newHomeState.copy(
                        workoutCompletionChart = if (newHomeState.workoutLogs.isEmpty()) null
                            else getWeeklyCompletionChart(
                                workoutCompletionRange = workoutCompletionRange,
                                workoutsInDateRange = newHomeState.workoutLogs.filterByDateRange(dateRange)
                            )
                    )
                } else newHomeState.copy(
                    workoutCompletionChart = currentHomeState.workoutCompletionChart
                )

                newHomeState = if (currentConfiguredMetricsState.volumeMetricChartData != newConfiguredMetricsState.volumeMetricChartData) {
                    newHomeState.copy(
                        volumeMetricChartModels =
                            newHomeState.lifts.toVolumeMetricChartModels(
                                groupedData = newConfiguredMetricsState.volumeMetricChartData
                                    .map { it.key to it.value.fastMap { workoutLog -> workoutLog.toUiModel() } }
                                    .toMap()
                            )
                    )
                } else newHomeState.copy(
                    volumeMetricChartModels = currentHomeState.volumeMetricChartModels
                )

                newHomeState = if (currentConfiguredMetricsState.liftMetricChartData != newConfiguredMetricsState.liftMetricChartData ||
                    currentConfiguredMetricsState.liftMetricCharts != newConfiguredMetricsState.liftMetricCharts) {
                    newHomeState.copy(
                        liftMetricChartModels =
                            newConfiguredMetricsState.liftMetricCharts.toChartModels(
                                groupedLogs = newConfiguredMetricsState.liftMetricChartData
                                    .map { it.key to it.value.fastMap { workoutLog -> workoutLog.toUiModel() } }
                                    .toMap()
                            )
                    )
                } else newHomeState.copy(
                    liftMetricChartModels = currentHomeState.liftMetricChartModels
                )

                newConfiguredMetricsState to newHomeState
            }.combine(firebaseAuthFlow) { (configurationState, homeState), firebaseUser ->
                homeState.copy(
                    firebaseUsername = firebaseUser?.email,
                    emailVerified = firebaseUser?.isEmailVerified ?: false,
                )
            }.map { newHomeState ->
                _state.update {
                    it.copy(
                        firebaseUsername = newHomeState.firebaseUsername,
                        emailVerified = newHomeState.emailVerified,
                        activeProgram = newHomeState.activeProgram,
                        lifts = newHomeState.lifts,
                        workoutLogs = newHomeState.workoutLogs,
                        workoutCompletionChart = newHomeState.workoutCompletionChart,
                        microCycleCompletionChart = newHomeState.microCycleCompletionChart,
                        volumeMetricChartModels = newHomeState.volumeMetricChartModels,
                        liftMetricChartModels = newHomeState.liftMetricChartModels,
                        liftMetricOptions = _state.value.liftMetricOptions ?: buildLiftMetricOptionsTree(),
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

    private fun buildLiftMetricOptionsTree(): LiftMetricOptionTree {

        val chartPickerActions = LiftMetricChartOptionActions(
            onSelectLiftForMetricCharts = ::selectLiftForMetricCharts,
            onUpdateLiftChartTypeSelections = ::updateLiftChartTypeSelections,
            onAddVolumeMetricChart = ::addVolumeMetricChart,
            onUpdateVolumeTypeImpactSelection = ::updateVolumeTypeImpactSelection,
            onUpdateVolumeTypeSelections = ::updateVolumeTypeSelections
        )
        val liftMetricOptionsTree = createLiftMetricChartOptions(
            actions = chartPickerActions
        )

        return liftMetricOptionsTree
    }

    fun toggleLoginModal() {
        _state.update {
            it.copy(
                loginModalVisible = !state.value.loginModalVisible
            )
        }
    }

    fun createAccount(email: String, password: String) = executeWithErrorHandling("Failed to create account.") {
        try {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.user?.sendEmailVerification()
                            ?: throw Exception("User is null after successful account creation.")
                    }
                    handleFirebaseTaskCompletion(task)
                }
        } catch (ex: Exception) {
            handleFirebaseError(ex)
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
        val charts = _state.value.volumeTypeSelections.fastMap { volumeTypeStr ->
            VolumeMetricChart(
                volumeType = VolumeType.fromDisplayName(volumeTypeStr),
                volumeTypeImpactSelection = _state.value.volumeImpactSelection?.toVolumeTypeImpact() ?: VolumeTypeImpactSelection.COMBINED
            )
        }
        upsertManyVolumeMetricChartsUseCase(charts)
        toggleLiftChartPicker()
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
        val charts = _state.value.liftChartTypeSelections.fastMap {
            LiftMetricChart(
                chartType = it.toLiftMetricChartType()
            )
        }
        val chartIds = insertManyLiftMetricChartsUseCase(charts)
        onNavigateToLiftLibrary(chartIds)
    }

    fun deleteLiftMetricChart(id: Long) = executeWithErrorHandling("Failed to delete lift metric chart") {
        deleteLiftMetricChartByIdUseCase(id)
    }

    fun deleteVolumeMetricChart(id: Long) = executeWithErrorHandling("Failed to delete volume metric chart") {
        deleteVolumeMetricChartByIdUseCase(id)
    }
}