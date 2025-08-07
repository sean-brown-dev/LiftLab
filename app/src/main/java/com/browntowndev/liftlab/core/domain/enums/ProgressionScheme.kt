package com.browntowndev.liftlab.core.domain.enums

/**
 * Defines the progression model for a lift.
 *
 * This enum is self-contained, holding all associated properties like display names
 * and business logic rules (e.g., whether it supports custom sets).
 */
enum class ProgressionScheme(
    val displayName: String,
    val shortName: String,
    val isLinearProgression: Boolean = false,
    val canHaveCustomSets: Boolean,
    val rpeLabel: String
) {
    WAVE_LOADING_PROGRESSION(
        displayName = "Wave Loading",
        shortName = "WL",
        canHaveCustomSets = false,
        rpeLabel = "Top Set RPE"
    ),
    LINEAR_PROGRESSION(
        displayName = "Linear Progression",
        shortName = "LP",
        isLinearProgression = true,
        canHaveCustomSets = false,
        rpeLabel = "Max RPE"
    ),
    DOUBLE_PROGRESSION(
        displayName = "Double Progression",
        shortName = "DP",
        canHaveCustomSets = true,
        rpeLabel = "RPE"
    ),
    DYNAMIC_DOUBLE_PROGRESSION(
        displayName = "Dynamic Double Progression",
        shortName = "DDP",
        canHaveCustomSets = true,
        rpeLabel = "RPE"
    );

    /**
     * Finds a [ProgressionScheme] by its [displayName], ignoring case.
     * @param name The display name to search for (e.g., "Dynamic Double Progression", "Wave Loading").
     * @return The matching [ProgressionScheme] or null if no match is found.
     * @throws IllegalArgumentException if the [name] does not match any known progression scheme.
     */
    companion object {
        fun fromDisplayName(name: String): ProgressionScheme =
            ProgressionScheme.entries.find { it.displayName.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown volume type: $this")
    }
}
