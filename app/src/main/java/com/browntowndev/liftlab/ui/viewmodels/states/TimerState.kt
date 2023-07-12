package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.DOUBLE_HOURS_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.DOUBLE_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.SINGLE_HOURS_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.ONE_HOUR_IN_MILLIS
import com.browntowndev.liftlab.core.common.SINGLE_MINUTES_SECONDS_FORMAT
import com.browntowndev.liftlab.core.common.TEN_HOURS_IN_MILLIS
import com.browntowndev.liftlab.core.common.TEN_MINUTES_IN_MILLIS
import com.browntowndev.liftlab.core.common.toTimeString

data class TimerState(
    val running: Boolean = false,
    val millisTicked: Long = 0L,
) {
    val time by lazy {
        millisTicked.toTimeString(
            if (millisTicked < TEN_MINUTES_IN_MILLIS) SINGLE_MINUTES_SECONDS_FORMAT
            else if (millisTicked < ONE_HOUR_IN_MILLIS ) DOUBLE_MINUTES_SECONDS_FORMAT
            else if (millisTicked < TEN_HOURS_IN_MILLIS) SINGLE_HOURS_MINUTES_SECONDS_FORMAT
            else DOUBLE_HOURS_MINUTES_SECONDS_FORMAT
        )
    }
}