package com.browntowndev.liftlab.core.domain.enums


/**
 * Represents the category of a [VolumeType].
 */
enum class VolumeTypeCategory {
    PRIMARY,
    SECONDARY,
}

/**
 * Selection option for charting/filtering volume types.
 */
enum class VolumeTypeImpactSelection(val bitmask: Int, val displayName: String) {
    PRIMARY(1, "Primary"),
    SECONDARY(2, "Secondary"),
    COMBINED(4, "All");

    /**
     * Finds a [VolumeTypeImpactSelection] by its [displayName], ignoring case.
     * @param name The display name to search for (e.g., "Primary", "secondary").
     * @return The matching [VolumeTypeImpactSelection] or null if no match is found.
     * @throws IllegalArgumentException if the [name] does not match any known volume type.
     */
    companion object {
        fun fromDisplayName(name: String): VolumeTypeImpactSelection =
            entries.find { it.displayName.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown volume type: $this")
    }
}

/**
 * Represents the type of volume a lift contributes to.
 */
enum class VolumeType(val bitMask: Int, val displayName: String) {
    CHEST(1, "Chest"),
    BACK(2, "Back"),
    QUAD(4, "Quads"),
    HAMSTRING(8, "Hamstrings"),
    GLUTE(16, "Glutes"),
    POSTERIOR_DELTOID(32, "Posterior Deltoids"),
    LATERAL_DELTOID(64, "Lateral Deltoids"),
    ANTERIOR_DELTOID(128, "Anterior Deltoids"),
    TRICEP(256, "Triceps"),
    BICEP(512, "Biceps"),
    TRAP(1024, "Traps"),
    CALF(2048, "Calves"),
    FOREARM(4096, "Forearms"),
    AB(8192, "Abs"),
    LOWER_BACK(16384, "Lower Back");

    companion object {
        /**
         * Finds a [VolumeType] by its [displayName], ignoring case.
         *
         * @param name The display name to search for (e.g., "Chest", "quads").
         * @return The matching [VolumeType] or null if no match is found.
         * @throws IllegalArgumentException if the [name] does not match any known volume type.
         */
        fun fromDisplayName(name: String): VolumeType =
            entries.find { it.displayName.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown volume type: $this")
    }
}

/**
 * Extension function to retrieve a list of [VolumeType] enums from an integer bitmask.
 */
fun Int.getVolumeTypes(): List<VolumeType> {
    return VolumeType.entries.filter { (this and it.bitMask) != 0 }
}
