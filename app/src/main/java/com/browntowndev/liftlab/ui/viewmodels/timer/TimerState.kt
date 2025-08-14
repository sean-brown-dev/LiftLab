package com.browntowndev.liftlab.ui.viewmodels.timer

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.toTimeString

@Stable
data class TimerState(
    val running: Boolean = false,
    val millisTicked: Long = 0L,
) {
    val time by lazy {
        millisTicked.toTimeString()
    }
}