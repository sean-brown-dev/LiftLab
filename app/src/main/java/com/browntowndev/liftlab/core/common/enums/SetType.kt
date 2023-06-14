package com.browntowndev.liftlab.core.common.enums

enum class SetType {
    STANDARD_SET,
    DROP_SET,
    MYOREP_SET,
}

fun SetType.displayName(): String {
    return when (this) {
        SetType.STANDARD_SET -> "Standard"
        SetType.DROP_SET -> "Drop Set"
        SetType.MYOREP_SET -> "Myo-Reps"
    }
}

fun SetType.displayNameShort(standardOverride: String = ""): String {
    return when (this) {
        SetType.STANDARD_SET -> standardOverride.ifEmpty { "S" }
        SetType.DROP_SET -> "D"
        SetType.MYOREP_SET -> "M"
    }
}