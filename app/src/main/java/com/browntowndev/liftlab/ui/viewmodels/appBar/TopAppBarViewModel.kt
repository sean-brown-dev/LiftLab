package com.browntowndev.liftlab.ui.viewmodels.appBar

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.domain.models.workoutLogging.RestTimerInProgressState
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetRestTimerInProgressFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.RestTimerCompletedUseCase
import com.browntowndev.liftlab.ui.models.controls.AppBarMutateControlRequest
import com.browntowndev.liftlab.ui.notifications.LiftLabTimer
import com.browntowndev.liftlab.ui.notifications.NotificationHelper
import com.browntowndev.liftlab.ui.viewmodels.appBar.screen.Screen
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TopAppBarViewModel(
    context: Context,
    getRestTimerInProgressFlowUseCase: GetRestTimerInProgressFlowUseCase,
    private val restTimerCompletedUseCase: RestTimerCompletedUseCase,
    private val liftLabTimer: LiftLabTimer,
): ViewModel()  {

    private val _state = MutableStateFlow(LiftLabTopAppBarState())
    val state = _state.asStateFlow()

    private val millisRemaining = MutableStateFlow<Long?>(null)

    private val restState: Flow<RestTimerInProgressState> = getRestTimerInProgressFlowUseCase()

    val timerState = combine(liftLabTimer.isRunning, restState) { isTimerRunning, rest ->
        Log.d("TopAppBarViewModel", "top flow is running: $isTimerRunning")
        Log.d("TopAppBarViewModel", "top flow rest: $rest")
        Log.d("TopAppBarViewModel", "hash=${rest.hashCode()} value=$rest")

        val millisRemaining =
            if (rest.totalRestTimeInMillis != null && rest.timeStartedInMillis != null) {
                val elapsedMs = getCurrentDate().time - rest.timeStartedInMillis
                (rest.totalRestTimeInMillis - elapsedMs).coerceAtLeast(0L)
            } else null

        Log.d("TopAppBarViewModel", "top flow millis remaining: $millisRemaining")
        CountdownTimerState(
            startDurationInMillis = rest.totalRestTimeInMillis,
            millisRemaining = millisRemaining,
            startTimeInMillis = rest.timeStartedInMillis,
            running = isTimerRunning
        )
    }.scan(CountdownTimerState()) { oldState, newState ->

        val finished = oldState.running && !newState.running
        val needsCancelled = newState.running && newState.millisRemaining == null
        val needsStarted = !newState.running && newState.millisRemaining != null
        val needsRestarted = newState.millisRemaining != null && oldState.startTimeInMillis != newState.startTimeInMillis

        Log.d("TopAppBarViewModel", "needsCancelled: $needsCancelled")
        Log.d("TopAppBarViewModel", "needsStarted: $needsStarted")
        Log.d("TopAppBarViewModel", "needsRestarted: $needsRestarted")

        // Compute whether it's running still based on the actions.
        val running = when {
            needsCancelled -> {
                cancelRestTimer()
                false
            }
            needsStarted || needsRestarted -> {
                startRestTimer(context, remainingMillis = newState.millisRemaining)
                true
            }
            finished -> false
            else -> oldState.running
        }

        val newStateWithRunningUpdated = newState.copy(
            running = running
        )

        newStateWithRunningUpdated
    }.combine(millisRemaining) { timerState, millisRemaining ->
        Log.d("TopAppBarViewModel", "live ticker flow millis remaining: $millisRemaining")
        val finalState = timerState.copy(
            millisRemaining = millisRemaining ?: timerState.millisRemaining
        )
        Log.d("TopAppBarViewModel", "live ticker flow final state: $finalState")
        finalState
    }.catch { e ->
        Log.e("TopAppBarViewModel", "Error getting rest timer in progress", e)
        FirebaseCrashlytics.getInstance().recordException(e)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0L),
        initialValue = CountdownTimerState()
    )

    fun startRestTimer(
        context: Context,
        remainingMillis: Long,
    ) {
        liftLabTimer.start(
            countDown = true,
            millisInFuture = remainingMillis,
            countDownInterval = 1000L,
            onTick = { millisecondsRemaining ->
                millisRemaining.value = millisecondsRemaining
            },
            onFinish = {
                finishRestTimerAndNotify(context)
            }
        )
    }

    private fun finishRestTimerAndNotify(context: Context) {
        try {
            Log.d("TopAppBarViewModel", "Rest timer finished.")
            cancelRestTimer()
            NotificationHelper.postRestTimerCompletionAlert(context)
            Log.d("TopAppBarViewModel", "!-------- NOTIFICATION SENT ------------!")
        } catch (e: Exception) {
            Log.e("TopAppBarViewModel", "Error finishing rest timer", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            // TODO: Emit user message
        }
    }

    fun cancelRestTimer() = viewModelScope.launch {
        try {
            liftLabTimer.cancel()
            millisRemaining.value = null
            restTimerCompletedUseCase()
        } catch (e: Exception) {
            Log.e("TopAppBarViewModel", "Error canceling rest timer", e)
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
        Log.d("TopAppBarViewModel", "screen=${_state.value.currentScreen?.route}, setControlVisibility: $controlName, $isVisible")
        _state.update { it.copy(currentScreen = it.currentScreen?.setControlVisibility(controlName, isVisible = isVisible)) }
    }
}
