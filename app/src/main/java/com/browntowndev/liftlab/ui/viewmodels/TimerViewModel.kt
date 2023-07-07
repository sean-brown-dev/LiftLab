package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.core.common.FIVE_HOURS_IN_MILLIS
import com.browntowndev.liftlab.core.common.LiftLabTimer
import com.browntowndev.liftlab.ui.viewmodels.states.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TimerViewModel: ViewModel() {
    private var _state = MutableStateFlow(TimerState())
    val state = _state.asStateFlow()
    private var _countDownTimer: LiftLabTimer? = null

    fun start() {
        if (_state.value.running) return
        startNewTimer()
    }

    private fun startNewTimer() {
        _state.update {
            it.copy(
                running = true,
                millisTicked = 0L
            )
        }

        _countDownTimer = object : LiftLabTimer(
            countDown = false,
            millisInFuture = FIVE_HOURS_IN_MILLIS,
            countDownInterval = 1000L
        ) {
            override fun onTick(newTimeInMillis: Long) {
                _state.update {
                    it.copy(
                        millisTicked = newTimeInMillis,
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
}