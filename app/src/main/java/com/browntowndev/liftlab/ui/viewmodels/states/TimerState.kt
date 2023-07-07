package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.DOUBLE_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.HOURS_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.ONE_HOUR_IN_MILLIS
import com.browntowndev.liftlab.core.common.SINGLE_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.TEN_MINUTES_IN_MILLIS
import com.browntowndev.liftlab.core.common.toMinutesSecondsString

data class TimerState(
    val running: Boolean = false,
    val millisTicked: Long = 0L,
) {
    val time by lazy {
        millisTicked.toMinutesSecondsString(
            if (millisTicked < TEN_MINUTES_IN_MILLIS) SINGLE_MINUTES_SECONDS_FORMAT
            else if (millisTicked < ONE_HOUR_IN_MILLIS ) DOUBLE_MINUTES_SECONDS_FORMAT
            else HOURS_MINUTES_SECONDS_FORMAT
        )
    }
}