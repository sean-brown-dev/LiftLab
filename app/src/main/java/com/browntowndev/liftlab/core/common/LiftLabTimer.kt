package com.browntowndev.liftlab.core.common

import android.os.CountDownTimer

abstract class LiftLabTimer(
    private val countDown: Boolean,
    private val millisInFuture: Long,
    private val countDownInterval: Long,
) {
    abstract fun onTick(newTimeInMillis: Long)
    abstract fun onFinish()

    private val _countdownTimer = object: CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisRemaining: Long) {
            val newTimeInMillis = if (countDown) {
                millisRemaining + (countDownInterval - (millisRemaining % countDownInterval))
            } else {
                millisInFuture - (millisRemaining + (countDownInterval - (millisRemaining % countDownInterval)))
            }

            this@LiftLabTimer.onTick(newTimeInMillis)
        }

        override fun onFinish() {
            this@LiftLabTimer.onFinish()
        }
    }

    fun cancel() {
        _countdownTimer.cancel()
    }

    fun start(): LiftLabTimer {
        _countdownTimer.start()
        return this
    }
}