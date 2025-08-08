package com.browntowndev.liftlab.core.domain.enums

/**
 * Defines the progression model for a lift.
 *
 * This enum is self-contained, holding all associated properties like display names
 * and business logic rules (e.g., whether it supports custom sets).
 */
enum class ProgressionScheme(
    val canHaveCustomSets: Boolean,
) {
    WAVE_LOADING_PROGRESSION(
        canHaveCustomSets = false,
    ),
    LINEAR_PROGRESSION(
        canHaveCustomSets = false,
    ),
    DOUBLE_PROGRESSION(
        canHaveCustomSets = true,
    ),
    DYNAMIC_DOUBLE_PROGRESSION(
        canHaveCustomSets = true,
    );
}
