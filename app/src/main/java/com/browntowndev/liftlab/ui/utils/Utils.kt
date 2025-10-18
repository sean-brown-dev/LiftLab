package com.browntowndev.liftlab.ui.utils

import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme

fun getRpeTargetPlaceholder(rpeTarget: Float, position: Int, progressionScheme: ProgressionScheme, isCustom: Boolean): String = when {
    rpeTarget == 10f -> ""
    position == 0 || isCustom -> rpeTarget.toString().removeSuffix(".0")
    else -> when (progressionScheme) {
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> rpeTarget.toString().removeSuffix(".0")
        ProgressionScheme.TOP_SET_PROGRESSION,
        ProgressionScheme.WAVE_LOADING_PROGRESSION,
        ProgressionScheme.DOUBLE_PROGRESSION,
        ProgressionScheme.LINEAR_PROGRESSION -> "≤${rpeTarget.toString().removeSuffix(".0")}"
    }
}