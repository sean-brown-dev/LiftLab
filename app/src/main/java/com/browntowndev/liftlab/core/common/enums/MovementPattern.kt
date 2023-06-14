package com.browntowndev.liftlab.core.common.enums

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
