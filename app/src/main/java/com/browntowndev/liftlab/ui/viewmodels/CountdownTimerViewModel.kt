package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.ui.notifications.LiftLabTimer
import com.browntowndev.liftlab.ui.notifications.NotificationHelper
import com.browntowndev.liftlab.ui.viewmodels.states.CountdownTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class CountdownTimerViewModel(
    private val liftLabTimer: LiftLabTimer,
    private val onComplete: () -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(CountdownTimerState())
    val state = _state.asStateFlow()

    init {
        liftLabTimer.isRunning.onEach { isRunning ->
            _state.update {
                it.copy(
                    running = isRunning,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun start(
        context: Context,
        timerStartTimeInMillis: Long,
        startDurationInMillis: Long
    ): Boolean {
        if (liftLabTimer.isRunning.value) return false

        val countdownStartTimeMs = timerStartTimeInMillis.toDate().time
        val elapsedMs = (getCurrentDate().time - countdownStartTimeMs)
        val remaining = (startDurationInMillis - elapsedMs).coerceAtLeast(0L)

        if (remaining == 0L) {
            finishAndNotify(context)
            return false
        }

        liftLabTimer.start(
            countDown = true,
            millisInFuture = remaining,
            countDownInterval = 1000L,
            onTick = { millisecondsRemaining ->
                _state.update { it.copy(millisRemaining = millisecondsRemaining) }
            },
            onFinish = {
                finishAndNotify(context)
            }
        )

        _state.update {
            it.copy(
                startDurationInMillis = startDurationInMillis,
                millisRemaining = remaining,
            )
        }
        return true
    }

    fun cancel() {
        liftLabTimer.cancel()
        setStateAsFinished()
        onComplete()
    }

    private fun finishAndNotify(context: Context) {
        setStateAsFinished()
        postCompletionAlert(context)
        onComplete()
    }

    private fun setStateAsFinished() {
        _state.update { it.copy(millisRemaining = 0L, startDurationInMillis = 0L) }
    }

    /**
     * Posts a notification when the countdown is complete. Uses same notification as RestTimerNotificationService
     */
    private fun postCompletionAlert(context: Context) {
        NotificationHelper.postRestTimerCompletionAlert(context)
    }

    override fun onCleared() {
        super.onCleared()
        liftLabTimer.cancel()
    }
}
