package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetRestTimerInProgressFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.RestTimerCompletedUseCase
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.viewmodels.states.LiftLabTopAppBarState
import com.browntowndev.liftlab.ui.viewmodels.states.screens.Screen
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TopAppBarViewModel(
    getRestTimerInProgressFlowUseCase: GetRestTimerInProgressFlowUseCase,
    private val restTimerCompletedUseCase: RestTimerCompletedUseCase,

): ViewModel() {
    private val _state = MutableStateFlow(LiftLabTopAppBarState())
    val state = _state.asStateFlow()

    init {
        getRestTimerInProgressFlowUseCase()
            .onEach { restTimeState ->
                _state.update {
                    it.copy(
                        timeStartedInMillis = restTimeState.timeStartedInMillis,
                        totalRestTime = restTimeState.totalRestTime,
                    )
                }
                Log.d("TopAppBarViewModel", "Rest time remaining: ${restTimeState.timeStartedInMillis}")
            }.catch {
                Log.e("TopAppBarViewModel", "Error getting rest timer in progress", it)
                FirebaseCrashlytics.getInstance().recordException(it)
                // TODO: Emit user message
            }.launchIn(viewModelScope)
    }

    fun completeRestTimer() = viewModelScope.launch {
        try {
            restTimerCompletedUseCase()
        } catch (e: Exception) {
            Log.e("TopAppBarViewModel", "Error completing rest timer", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            // TODO: Emit user message
        }
    }

    fun setScreen(screen: Screen) {
        _state.update {
            it.copy(currentScreen = screen)
        }
    }

    fun setCollapsed(collapsed: Boolean) {
        if (collapsed != _state.value.isCollapsed) {
            _state.update {
                it.copy(isCollapsed = collapsed)
            }
        }
    }

    fun <T> mutateControlValue(request: AppBarMutateControlRequest<T>) {
        _state.update { it.copy(currentScreen = it.currentScreen?.mutateControlValue(request)) }
    }

    fun setControlVisibility(controlName: String, isVisible: Boolean) {
        _state.update { it.copy(currentScreen = it.currentScreen?.setControlVisibility(controlName, isVisible = isVisible)) }
    }
}

