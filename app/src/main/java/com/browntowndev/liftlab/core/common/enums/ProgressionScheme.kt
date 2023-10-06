package com.browntowndev.liftlab.core.common.enums

enum class ProgressionScheme {
    WAVE_LOADING_PROGRESSION,
    LINEAR_PROGRESSION,
    DOUBLE_PROGRESSION,
    DYNAMIC_DOUBLE_PROGRESSION,
}

fun ProgressionScheme.displayName(): String {
    return when(this) {
        ProgressionScheme.DOUBLE_PROGRESSION -> "Double Progression"
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> "Wave Loading"
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "Dynamic Double Progression"
        ProgressionScheme.LINEAR_PROGRESSION -> "Linear Progression"
    }
}

fun ProgressionScheme.displayNameShort(): String {
    return when(this) {
        ProgressionScheme.DOUBLE_PROGRESSION -> "DP"
        ProgressionScheme.WAVE_LOADING_PROGRESSION -> "WL"
        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> "DDP"
        ProgressionScheme.LINEAR_PROGRESSION -> "LP"
    }
}