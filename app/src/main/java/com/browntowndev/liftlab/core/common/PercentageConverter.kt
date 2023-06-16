package com.browntowndev.liftlab.core.common

fun convertToDouble(percentageString: String): Double {
    return percentageString.removeSuffix("%").toDouble() / 100
}