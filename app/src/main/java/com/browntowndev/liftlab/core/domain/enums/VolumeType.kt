package com.browntowndev.liftlab.core.domain.enums


enum class VolumeTypeCategory {
    PRIMARY,
    SECONDARY,
}

enum class VolumeTypeImpact(val bitmask: Int) {
    PRIMARY(1),
    SECONDARY(2),
    COMBINED(4),
}

enum class VolumeType(val bitMask: Int) {
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
    LOWER_BACK(16384),
}

fun Int.getVolumeTypes(): List<VolumeType> {
    return VolumeType.entries.filter { (this and it.bitMask) != 0 }
}
