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
enum class VolumeTypeImpactSelection(val bitmask: Int) {
    PRIMARY(1),
    SECONDARY(2),
    COMBINED(4);
}

/**
 * Represents the type of volume a lift contributes to.
 */
enum class VolumeType(val bitMask: Int ) {
    CHEST(1),
    BACK(2),
    QUAD(4),
    HAMSTRING(8),
    GLUTE(16),
    POSTERIOR_DELTOID(32),
    LATERAL_DELTOID(64),
    ANTERIOR_DELTOID(128),
    TRICEP(256),
    BICEP(512),
    TRAP(1024),
    CALF(2048),
    FOREARM(4096),
    AB(8192),
    LOWER_BACK(16384);
}

/**
 * Extension function to retrieve a list of [VolumeType] enums from an integer bitmask.
 */
fun Int.toVolumeTypes(): List<VolumeType> {
    return VolumeType.entries.filter { (this and it.bitMask) != 0 }
}
