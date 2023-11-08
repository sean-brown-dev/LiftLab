package com.browntowndev.liftlab.core.common


class FilterChipOption (val type: String, val value: String) {
    companion object {
        const val MOVEMENT_PATTERN = "Movement Pattern"
        const val NAME = "Name"
        const val PROGRAM = "Program"
        const val WORKOUT = "Workout"
        const val DATE_RANGE = "Date Range"
    }
}