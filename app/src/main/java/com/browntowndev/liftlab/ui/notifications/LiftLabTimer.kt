package com.browntowndev.liftlab.ui.notifications

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LiftLabTimer {
    private var timer: CountDownTimer? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    @Synchronized
    fun start(
        countDown: Boolean,
        millisInFuture: Long,
        countDownInterval: Long,
        onTick: (newTimeInMillis: Long) -> Unit,
        onFinish: () -> Unit ={ },
    ): LiftLabTimer {
        // Has to run on main looper
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                start(countDown, millisInFuture, countDownInterval, onTick, onFinish)
            }
            return this
        }

        // Kill any existing timer first
        timer?.cancel()

        timer = object : CountDownTimer(millisInFuture, countDownInterval) {
            override fun onTick(millisRemaining: Long) {
                val nextAligned = countDownInterval - (millisRemaining % countDownInterval)
                val newTimeInMillis = if (countDown) {
                    millisRemaining + nextAligned
                } else {
                    millisInFuture - (millisRemaining + nextAligned)
                }
                onTick(newTimeInMillis)
            }

            override fun onFinish() {
                timer = null
                _isRunning.value = false
                onFinish()
            }
        }.start()

        _isRunning.value = true
        return this
    }

    @Synchronized
    fun cancel() {
        timer?.cancel()
        timer = null
        _isRunning.value = false
    }
}
