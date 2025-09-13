package com.browntowndev.liftlab.core.domain.enums

/**
 * Defines the progression model for a lift.
 *
 * This enum is self-contained, holding all associated properties like display names
 * and business logic rules (e.g., whether it supports custom sets).
 */
enum class ProgressionScheme(
    val canHaveCustomSets: Boolean,
    val canVolumeCycle: Boolean,
) {
    WAVE_LOADING_PROGRESSION(
        canHaveCustomSets = false,
        canVolumeCycle = false,
    ),
    LINEAR_PROGRESSION(
        canHaveCustomSets = false,
        canVolumeCycle = false,
    ),
    DOUBLE_PROGRESSION(
        canHaveCustomSets = true,
        canVolumeCycle = true,
    ),
    DYNAMIC_DOUBLE_PROGRESSION(
        canHaveCustomSets = true,
        canVolumeCycle = true,
    );
}
