package com.browntowndev.liftlab.ui.utils

import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme

fun getRpeTargetPlaceholder(rpeTarget: Float, position: Int, progressionScheme: ProgressionScheme): String = when {
    rpeTarget == 10f -> ""
    position == 0 -> rpeTarget.toString().removeSuffix(".0")
    else -> when (progressionScheme) {
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> ""
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> rpeTarget.toString().removeSuffix(".0")
        ProgressionScheme.DOUBLE_PROGRESSION,
        ProgressionScheme.LINEAR_PROGRESSION -> "≤${rpeTarget.toString().removeSuffix(".0")}"
    }
}