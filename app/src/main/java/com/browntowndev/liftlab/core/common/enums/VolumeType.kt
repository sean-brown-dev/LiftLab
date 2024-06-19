package com.browntowndev.liftlab.core.common.enums

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

fun String.toVolumeType(): VolumeType {
    return when (this) {
        VolumeType.CHEST.displayName() -> VolumeType.CHEST
        VolumeType.BACK.displayName() -> VolumeType.BACK
        VolumeType.QUAD.displayName() -> VolumeType.QUAD
        VolumeType.HAMSTRING.displayName() -> VolumeType.HAMSTRING
        VolumeType.GLUTE.displayName() -> VolumeType.GLUTE
        VolumeType.POSTERIOR_DELTOID.displayName() -> VolumeType.POSTERIOR_DELTOID
        VolumeType.LATERAL_DELTOID.displayName() -> VolumeType.LATERAL_DELTOID
        VolumeType.ANTERIOR_DELTOID.displayName() -> VolumeType.ANTERIOR_DELTOID
        VolumeType.TRICEP.displayName() -> VolumeType.TRICEP
        VolumeType.BICEP.displayName() -> VolumeType.BICEP
        VolumeType.TRAP.displayName() -> VolumeType.TRAP
        VolumeType.CALF.displayName() -> VolumeType.CALF
        VolumeType.FOREARM.displayName() -> VolumeType.FOREARM
        VolumeType.AB.displayName() -> VolumeType.AB
        VolumeType.LOWER_BACK.displayName() -> VolumeType.LOWER_BACK
        else -> throw IllegalArgumentException("Unknown volume type: $this")
    }
}

fun String.toVolumeTypeImpact(): VolumeTypeImpact {
    return when (this) {
        VolumeTypeImpact.PRIMARY.displayName() -> VolumeTypeImpact.PRIMARY
        VolumeTypeImpact.SECONDARY.displayName() -> VolumeTypeImpact.SECONDARY
        VolumeTypeImpact.COMBINED.displayName() -> VolumeTypeImpact.COMBINED
        else -> throw IllegalArgumentException("Unknown volume type impact: $this")
    }
}

fun Int.getVolumeTypes(): List<VolumeType> {
    return VolumeType.entries.filter { (this and it.bitMask) != 0 }
}

fun Int.getVolumeTypeImpacts(): List<VolumeTypeImpact> {
    return VolumeTypeImpact.entries.filter { (this and it.bitmask) != 0 }
}

fun VolumeType.displayName(): String {
    return when (this) {
        VolumeType.CHEST -> "Chest"
        VolumeType.BACK -> "Back"
        VolumeType.QUAD -> "Quads"
        VolumeType.HAMSTRING -> "Hamstrings"
        VolumeType.GLUTE -> "Glutes"
        VolumeType.POSTERIOR_DELTOID -> "Posterior Deltoids"
        VolumeType.LATERAL_DELTOID -> "Lateral Deltoids"
        VolumeType.ANTERIOR_DELTOID -> "Anterior Deltoids"
        VolumeType.TRICEP -> "Triceps"
        VolumeType.BICEP -> "Biceps"
        VolumeType.TRAP -> "Traps"
        VolumeType.CALF -> "Calves"
        VolumeType.FOREARM -> "Forearms"
        VolumeType.AB -> "Abs"
        VolumeType.LOWER_BACK -> "Lower Back"
    }
}

fun VolumeTypeImpact.displayName(): String {
    return when (this) {
        VolumeTypeImpact.PRIMARY -> "Primary"
        VolumeTypeImpact.SECONDARY -> "Secondary"
        VolumeTypeImpact.COMBINED -> "All"
    }
}
