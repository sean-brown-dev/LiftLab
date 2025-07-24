package com.browntowndev.liftlab.core.common


data class FilterChipOption (val type: String, val value: String, val key: Long? = null) {
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (key?.hashCode() ?: 0)

        return result
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is FilterChipOption -> {
                type == other.type &&
                        value == other.value &&
                        key == other.key
            }
            else -> false
        }
    }
    companion object {
        const val MOVEMENT_PATTERN = "Movement Pattern"
        const val NAME = "Name"
        const val PROGRAM = "Program"
        const val WORKOUT = "Workout"
        const val DATE_RANGE = "Date Range"
    }
}