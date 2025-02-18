package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.browntowndev.liftlab.core.common.LiftLabTimer
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.ui.viewmodels.states.CountdownTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CountdownTimerViewModel(
    private val onComplete: (ranToCompletion: Boolean) -> Unit,
): ViewModel() {
    private var _state = MutableStateFlow(CountdownTimerState())
    val state = _state.asStateFlow()
    private var _countDownTimer: LiftLabTimer? = null

    fun start(
        timerId: String,
        originalCountDownStartedFrom: Long,
        startTimeInMillis: Long
    ): Boolean {
        // If this isn't a new request then don't restart the timer
        if (timerId == state.value.timerId) {
            return false
        }

        if (_countDownTimer !=  null) {
            cancelWithoutCallback()
        }

        _state.update {
            it.copy(
                timerId = timerId,
                running = true,
                originalCountDownStartedFrom = originalCountDownStartedFrom,
                startTimeInMillis = startTimeInMillis,
                millisRemaining = startTimeInMillis,
            )
        }

        _countDownTimer = object : LiftLabTimer(
            countDown = true,
            millisInFuture = startTimeInMillis,
            countDownInterval = 100L,
        ) {
            override fun onTick(newTimeInMillis: Long) {
                _state.update {
                    it.copy(
                        millisRemaining = newTimeInMillis,
                    )
                }
            }

            override fun onFinish() {
                _state.update {
                    it.copy(
                        originalCountDownStartedFrom = 0L,
                        millisRemaining = 0L,
                        running = false,
                    )
                }

                _state.update {
                    it.copy(
                        startTimeInMillis = 0L,
                    )
                }

                onComplete(true)
            }
        }.start()

        return true
    }

    private fun cancelWithoutCallback() {
        _countDownTimer?.cancel()
        _state.update {
            it.copy(
                running = false,
                startTimeInMillis = 0L,
                millisRemaining = 0L,
                originalCountDownStartedFrom = 0L,
            )
        }
    }

    fun cancel() {
        cancelWithoutCallback()
        onComplete(false)
    }
}