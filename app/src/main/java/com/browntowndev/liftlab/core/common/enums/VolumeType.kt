package com.browntowndev.liftlab.core.common.enums

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
    return VolumeType.values().filter { (this and it.bitMask) != 0 }
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