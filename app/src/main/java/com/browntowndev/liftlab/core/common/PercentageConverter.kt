package com.browntowndev.liftlab.core.common

fun convertToFloat(percentageString: String): Float {
    return percentageString.removeSuffix("%").toFloat() / 100
}