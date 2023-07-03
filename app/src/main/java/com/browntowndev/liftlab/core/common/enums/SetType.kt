package com.browntowndev.liftlab.core.common.enums

enum class SetType {
    STANDARD,
    DROP_SET,
    MYOREP,
}

fun SetType.displayName(): String {
    return when (this) {
        SetType.STANDARD -> "Standard"
        SetType.DROP_SET -> "Drop Set"
        SetType.MYOREP -> "Myo-Reps"
    }
}

fun SetType.displayNameShort(standardOverride: String = ""): String {
    return when (this) {
        SetType.STANDARD -> standardOverride.ifEmpty { "S" }
        SetType.DROP_SET -> "D"
        SetType.MYOREP -> "M"
    }
}