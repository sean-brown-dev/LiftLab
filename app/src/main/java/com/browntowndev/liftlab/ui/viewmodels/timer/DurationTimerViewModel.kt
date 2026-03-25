package com.browntowndev.liftlab.ui.viewmodels.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.MAX_TIME_IN_WHOLE_MILLISECONDS
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.ui.notifications.LiftLabTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.Date

class DurationTimerViewModel(
    private val liftLabTimer: LiftLabTimer,
): ViewModel() {
    private var _state = MutableStateFlow(TimerState())
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

    fun startFrom(initialStartTime: Date) {
        if (_state.value.running) return
        val millisSinceStart = getCurrentDate().time - initialStartTime.time
        startNewTimer(millisTicked = millisSinceStart)
    }

    private fun startNewTimer(millisTicked: Long = 0L) {
        _state.update {
            it.copy(
                millisTicked = millisTicked
            )
        }

        liftLabTimer.start(
            countDown = false,
            millisInFuture = MAX_TIME_IN_WHOLE_MILLISECONDS,
            countDownInterval = 1000L,
            onTick = { newTimeInMillis ->
                _state.update {
                    it.copy(
                        millisTicked = millisTicked + newTimeInMillis,
                    )
                }
            }
        )
    }

    fun stop() {
        liftLabTimer.cancel()

        _state.update {
            it.copy(
                running = false,
                millisTicked = 0L,
            )
        }
    }
}