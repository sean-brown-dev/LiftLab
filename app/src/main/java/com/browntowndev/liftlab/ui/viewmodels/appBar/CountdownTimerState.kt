package com.browntowndev.liftlab.ui.viewmodels.appBar

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.toTimeString
import kotlin.math.absoluteValue

@Stable
data class CountdownTimerState(
    val startDurationInMillis: Long? = null,
    val millisRemaining: Long? = null,
    val startTimeInMillis: Long? = null,
    val running: Boolean = false,
) {
    val timeRemaining: String by lazy {
        millisRemaining?.toTimeString() ?: ""
    }

    val progress: Float by lazy {
        if (millisRemaining == null || startDurationInMillis == null) 0f
        else {
            ((millisRemaining.toFloat() / startDurationInMillis) - 1).absoluteValue
        }
    }
}