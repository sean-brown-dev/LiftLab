package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.ui.models.TopAppBarEvent
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.SettingKey
import com.browntowndev.liftlab.core.domain.useCase.settings.GetSettingConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.settings.UpdateLiftSpecificDeloadSettingUseCase
import com.browntowndev.liftlab.core.domain.useCase.settings.UpdateSettingUseCase
import com.browntowndev.liftlab.ui.viewmodels.states.SettingsState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration

class SettingsViewModel(
    getSettingConfigurationStateFlowUseCase: GetSettingConfigurationStateFlowUseCase,
    private val updateLiftSpecificDeloadSettingUseCase: UpdateLiftSpecificDeloadSettingUseCase,
    private val updateSettingUseCase: UpdateSettingUseCase,
    private val onNavigateBack: () -> Unit,
    eventBus: EventBus,
): BaseViewModel(eventBus) {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        getSettingConfigurationStateFlowUseCase()
            .map { configurationState ->
                SettingsState(
                    defaultRestTime = configurationState.defaultRestTime,
                    defaultIncrement = configurationState.defaultIncrement,
                    activeProgram = configurationState.activeProgram,
                    liftSpecificDeloading = configurationState.liftSpecificDeloading,
                    promptOnDeloadStart = configurationState.promptOnDeloadStart,
                    useAllLiftDataForRecommendations = configurationState.useAllLiftDataForRecommendations,
                    useOnlyResultsFromLiftInSamePosition = configurationState.useOnlyResultsFromLiftInSamePosition,
                )
            }.onEach { state ->
                _state.update {
                    it.copy(
                        activeProgram = state.activeProgram ?: it.activeProgram,
                        defaultRestTime = state.defaultRestTime ?: it.defaultRestTime,
                        defaultIncrement = state.defaultIncrement ?: it.defaultIncrement,
                        liftSpecificDeloading = state.liftSpecificDeloading,
                        promptOnDeloadStart = state.promptOnDeloadStart,
                        useAllLiftDataForRecommendations = state.useAllLiftDataForRecommendations,
                        useOnlyResultsFromLiftInSamePosition = state.useOnlyResultsFromLiftInSamePosition,
                    )
                }
            }.catch {
                Log.e("SettingsViewModel", "Error getting settings state", it)
                FirebaseCrashlytics.getInstance().recordException(it)
                emitUserMessage("Failed to load Settings")
            }.launchIn(viewModelScope)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> {
                handleBackButtonPress()
            }
            else -> { }
        }
    }

    private fun handleBackButtonPress() {
        if (_state.value.isDonateScreenVisible) {
            toggleDonationScreen()
        } else {
            onNavigateBack()
        }
    }

    fun toggleDonationScreen() = executeWithErrorHandling("Failed to toggle donation screen") {
        _state.update {
            it.copy(isDonateScreenVisible = !it.isDonateScreenVisible)
        }
    }

    fun updateDefaultRestTime(restTime: Duration) = executeWithErrorHandling("Failed to update rest time") {
        updateSettingUseCase(SettingKey.RestTime, restTime.inWholeMilliseconds)
    }

    fun updateIncrement(increment: Float) = executeWithErrorHandling("Failed to update increment") {
        updateSettingUseCase(SettingKey.Increment, increment)
    }

    fun handleUseAllDataForRecommendationsChange(useOnlyFromPreviousWorkout: Boolean) = executeWithErrorHandling("Failed to update use all data for recommendations") {
        updateSettingUseCase(
            SettingKey.UseAllLiftDataForRecommendations,
            !useOnlyFromPreviousWorkout
        )
    }

    fun handleUseOnlyLiftsFromSamePositionChange(useOnlyLiftsFromSamePosition: Boolean) = executeWithErrorHandling("Failed to update use only lifts from same position") {
        updateSettingUseCase(
            SettingKey.UseOnlyResultsFromLiftInSamePosition,
            useOnlyLiftsFromSamePosition
        )
    }

    fun handlePromptForDeloadWeekChange(promptOnDeloadStart: Boolean) = executeWithErrorHandling("Failed to update prompt for deload week") {
        updateSettingUseCase(
            SettingKey.PromptForDeloadWeek,
            promptOnDeloadStart
        )
    }

    fun handleLiftSpecificDeloadChange(useLiftSpecificDeload: Boolean) = executeWithErrorHandling("Failed to update lift specific deload") {
        updateLiftSpecificDeloadSettingUseCase(_state.value.activeProgram!!, useLiftSpecificDeload)
    }
}