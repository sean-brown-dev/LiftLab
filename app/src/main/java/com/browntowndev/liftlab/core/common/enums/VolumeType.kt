package com.browntowndev.liftlab.core.common.enums

enum class VolumeType {
    CHEST,
    BACK,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    DELTOIDS,
    TRICEPS,
    BICEPS,
    TRAPS,
    CALVES,
    FOREARMS,
    ABS,
}

fun LiftCategory.volumeType(): VolumeType {
    return when (this) {
        LiftCategory.HORIZONTAL_PUSH, LiftCategory.CHEST_ISO -> VolumeType.CHEST
        LiftCategory.HORIZONTAL_PULL, LiftCategory.VERTICAL_PULL -> VolumeType.BACK
        LiftCategory.LEG_PUSH_COMPOUND, LiftCategory.QUAD_ISO -> VolumeType.QUADS
        LiftCategory.HIP_COMPOUND, LiftCategory.HAMSTRING_ISO -> VolumeType.HAMSTRINGS
        LiftCategory.BICEP_ISO -> VolumeType.BICEPS
        LiftCategory.TRICEP_ISO -> VolumeType.TRICEPS
        LiftCategory.DELT_ISO -> VolumeType.DELTOIDS
        LiftCategory.CALVES -> VolumeType.CALVES
        LiftCategory.GLUTE_ISO -> VolumeType.GLUTES
        LiftCategory.ABS_ISO -> VolumeType.ABS
        LiftCategory.FOREARM_ISO -> VolumeType.FOREARMS
        LiftCategory.TRAP_ISO -> VolumeType.TRAPS
    }
}