package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.DOUBLE_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.SINGLE_HOURS_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.ONE_HOUR_IN_MILLIS
import com.browntowndev.liftlab.core.common.SINGLE_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.TEN_MINUTES_IN_MILLIS
import com.browntowndev.liftlab.core.common.toTimeString
import kotlin.math.absoluteValue

data class CountdownTimerState(
    val running: Boolean = false,
    val startTimeInMillis: Long = 0L,
    val millisRemaining: Long = 0L,
) {
    val timeRemaining: String by lazy {
        millisRemaining.toTimeString(
            if (millisRemaining < TEN_MINUTES_IN_MILLIS) SINGLE_MINUTES_SECONDS_FORMAT
            else if (millisRemaining < ONE_HOUR_IN_MILLIS ) DOUBLE_MINUTES_SECONDS_FORMAT
            else SINGLE_HOURS_MINUTES_SECONDS_FORMAT
        )
    }
    val progress: Float by lazy {
        if(startTimeInMillis > 0L) {
            ((millisRemaining.toFloat() / startTimeInMillis) - 1).absoluteValue
        } else {
            0f
        }
    }
}