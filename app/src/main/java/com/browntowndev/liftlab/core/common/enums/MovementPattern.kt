package com.browntowndev.liftlab.core.common.enums

import com.browntowndev.liftlab.core.common.FlowRowFilterChipSection
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

enum class MovementPattern {
    LEG_PUSH,
    HIP_HINGE,
    QUAD_ISO,
    HAMSTRING_ISO,
    CALVES,

    HORIZONTAL_PUSH,
    VERTICAL_PULL,
    HORIZONTAL_PULL,
    CHEST_ISO,
    TRICEP_ISO,
    BICEP_ISO,
    DELT_ISO,

    GLUTE_ISO,
    FOREARM_ISO,
    TRAP_ISO,
    AB_ISO,
    INCLINE_PUSH,
    VERTICAL_PUSH,
}

sealed class MovementPatternFilterSection(
    override val sectionName: String,
    override val filterChipOptions: Lazy<List<String>>,
): FlowRowFilterChipSection {
    object UpperCompound: MovementPatternFilterSection(
        sectionName = "Upper Compound",
        filterChipOptions = lazy {
            listOf(
                MovementPattern.HORIZONTAL_PULL.displayName(),
                MovementPattern.VERTICAL_PULL.displayName(),
                MovementPattern.HORIZONTAL_PUSH.displayName(),
                MovementPattern.INCLINE_PUSH.displayName(),
                MovementPattern.VERTICAL_PUSH.displayName(),
            )
        }
    )

    object UpperAccessory: MovementPatternFilterSection(
        sectionName = "Upper Accessory",
        filterChipOptions = lazy {
            listOf(
                MovementPattern.CHEST_ISO.displayName(),
                MovementPattern.TRICEP_ISO.displayName(),
                MovementPattern.BICEP_ISO.displayName(),
                MovementPattern.DELT_ISO.displayName(),
                MovementPattern.FOREARM_ISO.displayName(),
                MovementPattern.TRAP_ISO.displayName(),
                MovementPattern.AB_ISO.displayName(),
            )
        }
    )

    object LowerCompound: MovementPatternFilterSection(
        sectionName = "Lower Compound",
        filterChipOptions = lazy {
            listOf(
                MovementPattern.HIP_HINGE.displayName(),
                MovementPattern.LEG_PUSH.displayName(),
            )
        }
    )

    object LowerAccessory: MovementPatternFilterSection(
        sectionName = "Lower Accessory",
        filterChipOptions = lazy {
            listOf(
                MovementPattern.QUAD_ISO.displayName(),
                MovementPattern.HAMSTRING_ISO.displayName(),
                MovementPattern.GLUTE_ISO.displayName(),
                MovementPattern.CALVES.displayName(),
            )
        }
    )
}

fun MovementPattern.displayName(): String {
    return when (this) {
        MovementPattern.BICEP_ISO -> "Bicep Isolation"
        MovementPattern.LEG_PUSH -> "Leg Push"
        MovementPattern.HIP_HINGE -> "Hip Hinge"
        MovementPattern.QUAD_ISO -> "Quad Isolation"
        MovementPattern.HAMSTRING_ISO -> "Hamstring Isolation"
        MovementPattern.CALVES -> "Calves"
        MovementPattern.HORIZONTAL_PUSH -> "Horizontal Push"
        MovementPattern.VERTICAL_PULL -> "Vertical Pull"
        MovementPattern.HORIZONTAL_PULL -> "Horizontal Pull"
        MovementPattern.CHEST_ISO -> "Chest Isolation"
        MovementPattern.TRICEP_ISO -> "Tricep Isolation"
        MovementPattern.DELT_ISO -> "Deltoid Isolation"
        MovementPattern.TRAP_ISO -> "Trap Isolation"
        MovementPattern.FOREARM_ISO -> "Forearm Isolation"
        MovementPattern.GLUTE_ISO -> "Glute Isolation"
        MovementPattern.AB_ISO -> "Abs"
        MovementPattern.INCLINE_PUSH -> "Incline Push"
        MovementPattern.VERTICAL_PUSH -> "Vertical Push"
    }
}

class LiftCategoryDeserializer : JsonDeserializer<MovementPattern> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MovementPattern {
        val intValue = json?.asInt
        return MovementPattern.values().find { it.ordinal == intValue } ?: throw Exception("$intValue is not a LiftCategory.")
    }
}
