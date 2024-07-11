package com.browntowndev.liftlab.core.common.enums

class VolumeTypeUtils {
    companion object {
        fun getDefaultVolumeTypes(movementPattern: MovementPattern): List<VolumeType> {
            return when (movementPattern) {
                MovementPattern.AB_ISO -> listOf(VolumeType.AB)
                MovementPattern.CALVES -> listOf(VolumeType.CALF)
                MovementPattern.DELT_ISO -> listOf(VolumeType.LATERAL_DELTOID)
                MovementPattern.BICEP_ISO -> listOf(VolumeType.BICEP)
                MovementPattern.CHEST_ISO -> listOf(VolumeType.CHEST)
                MovementPattern.GLUTE_ISO -> listOf(VolumeType.GLUTE)
                MovementPattern.HAMSTRING_ISO -> listOf(VolumeType.HAMSTRING)
                MovementPattern.QUAD_ISO -> listOf(VolumeType.QUAD)
                MovementPattern.TRICEP_ISO -> listOf(VolumeType.TRICEP)
                MovementPattern.TRAP_ISO -> listOf(VolumeType.TRAP)
                MovementPattern.FOREARM_ISO -> listOf(VolumeType.FOREARM)
                MovementPattern.LEG_PUSH -> listOf(VolumeType.QUAD)
                MovementPattern.HIP_HINGE -> listOf(VolumeType.HAMSTRING)
                MovementPattern.VERTICAL_PULL -> listOf(VolumeType.BACK)
                MovementPattern.HORIZONTAL_PULL -> listOf(VolumeType.BACK)
                MovementPattern.VERTICAL_PUSH -> listOf(VolumeType.ANTERIOR_DELTOID)
                MovementPattern.INCLINE_PUSH -> listOf(VolumeType.CHEST)
                MovementPattern.HORIZONTAL_PUSH -> listOf(VolumeType.CHEST)
            }
        }

        fun getDefaultSecondaryVolumeTypes(movementPattern: MovementPattern): List<VolumeType>? {
            return when (movementPattern) {
                MovementPattern.AB_ISO -> null
                MovementPattern.CALVES -> null
                MovementPattern.DELT_ISO -> null
                MovementPattern.BICEP_ISO -> null
                MovementPattern.CHEST_ISO -> listOf(VolumeType.TRICEP)
                MovementPattern.GLUTE_ISO -> null
                MovementPattern.HAMSTRING_ISO -> null
                MovementPattern.QUAD_ISO -> null
                MovementPattern.TRICEP_ISO -> null
                MovementPattern.TRAP_ISO -> null
                MovementPattern.FOREARM_ISO -> null
                MovementPattern.LEG_PUSH -> listOf(VolumeType.GLUTE)
                MovementPattern.HIP_HINGE -> listOf(VolumeType.GLUTE)
                MovementPattern.VERTICAL_PULL -> listOf(VolumeType.BICEP)
                MovementPattern.HORIZONTAL_PULL -> listOf(VolumeType.BICEP)
                MovementPattern.VERTICAL_PUSH -> null
                MovementPattern.INCLINE_PUSH -> listOf(VolumeType.TRICEP)
                MovementPattern.HORIZONTAL_PUSH -> listOf(VolumeType.TRICEP)
            }
        }
    }
}