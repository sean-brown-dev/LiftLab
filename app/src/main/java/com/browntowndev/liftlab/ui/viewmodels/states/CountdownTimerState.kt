package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.toTimeString
import kotlin.math.absoluteValue

data class CountdownTimerState(
    val running: Boolean = false,
    val startDurationInMillis: Long = 0L,
    val millisRemaining: Long = 0L,
    val startTimeInMillis: Long = 0L,
) {
    val timeRemaining: String by lazy {
        millisRemaining.toTimeString()
    }
    val progress: Float by lazy {
        if(startDurationInMillis > 0L) {
            ((millisRemaining.toFloat() / startDurationInMillis) - 1).absoluteValue
        } else {
            0f
        }
    }
}