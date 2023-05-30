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

fun Int.getVolumeType(): List<VolumeType> {
    return VolumeType.values().filter { (this and it.bitMask) != 0 }
}

fun VolumeType.displayName(): String {
    return when (this) {
        VolumeType.CHEST -> "Chest"
        VolumeType.BACK -> "Back"
        VolumeType.QUAD -> "Quad"
        VolumeType.HAMSTRING -> "Hamstring"
        VolumeType.GLUTE -> "Glute"
        VolumeType.POSTERIOR_DELTOID -> "Posterior Deltoid"
        VolumeType.LATERAL_DELTOID -> "Lateral Deltoid"
        VolumeType.ANTERIOR_DELTOID -> "Anterior Deltoid"
        VolumeType.TRICEP -> "Tricep"
        VolumeType.BICEP -> "Bicep"
        VolumeType.TRAP -> "Trap"
        VolumeType.CALF -> "Calf"
        VolumeType.FOREARM -> "Forearm"
        VolumeType.AB -> "Ab"
        VolumeType.LOWER_BACK -> "Lower Back"
    }
}