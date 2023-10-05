package com.browntowndev.liftlab.core.common.enums

enum class ProgressionScheme {
    WAVE_LOADING_PROGRESSION,
    LINEAR_PROGRESSION,
    DOUBLE_PROGRESSION_TOP_SET_RPE,
    DYNAMIC_DOUBLE_PROGRESSION,
    DOUBLE_PROGRESSION_REP_RANGE,
}

fun ProgressionScheme.displayName(): String {
    return when(this) {
        ProgressionScheme.DOUBLE_PROGRESSION_TOP_SET_RPE -> "Double Progression - Top Set RPE"
        ProgressionScheme.DOUBLE_PROGRESSION_REP_RANGE -> "Double Progression - Rep Range"
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> "Wave Loading"
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "Dynamic Double Progression"
        ProgressionScheme.LINEAR_PROGRESSION -> "Linear Progression"
    }
}

fun ProgressionScheme.displayNameShort(): String {
    return when(this) {
        ProgressionScheme.DOUBLE_PROGRESSION_TOP_SET_RPE,
        ProgressionScheme.DOUBLE_PROGRESSION_REP_RANGE -> "DP"
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> "WL"
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "DDP"
        ProgressionScheme.LINEAR_PROGRESSION -> "LP"
    }
}