package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.toTimeString

data class TimerState(
    val running: Boolean = false,
    val millisTicked: Long = 0L,
) {
    val time by lazy {
        millisTicked.toTimeString()
    }
}