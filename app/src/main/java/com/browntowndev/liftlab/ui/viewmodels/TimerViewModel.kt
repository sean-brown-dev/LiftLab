package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.core.common.LiftLabTimer
import com.browntowndev.liftlab.core.common.MAX_TIME_IN_WHOLE_MILLISECONDS
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.ui.viewmodels.states.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

class TimerViewModel: ViewModel() {
    private var _state = MutableStateFlow(TimerState())
    val state = _state.asStateFlow()
    private var _countDownTimer: LiftLabTimer? = null

    fun start() {
        if (_state.value.running) return
        startNewTimer()
    }

    fun startFrom(startTime: Date) {
        if (_state.value.running) return
        val millisSinceStart = getCurrentDate().time - startTime.time
        startNewTimer(millisTicked = millisSinceStart)
    }

    private fun startNewTimer(millisTicked: Long = 0L) {
        _state.update {
            it.copy(
                running = true,
                millisTicked = millisTicked
            )
        }

        _countDownTimer = object : LiftLabTimer(
            countDown = false,
            millisInFuture = MAX_TIME_IN_WHOLE_MILLISECONDS,
            countDownInterval = 1000L
        ) {
            override fun onTick(newTimeInMillis: Long) {
                _state.update {
                    it.copy(
                        millisTicked = millisTicked + newTimeInMillis,
                    )
                }
            }

            override fun onFinish() {
                _state.update {
                    it.copy(
                        running = false,
                    )
                }
            }
        }.start()
    }

    fun stop() {
        _countDownTimer?.cancel()

        _state.update {
            it.copy(
                running = false,
                millisTicked = 0L,
            )
        }
    }
}