package com.browntowndev.liftlab.core.common.enums

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

enum class LiftCategory {
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

fun LiftCategory.displayName(): String {
    return when (this) {
        LiftCategory.BICEP_ISO -> "Bicep Isolation"
        LiftCategory.LEG_PUSH -> "Leg Push"
        LiftCategory.HIP_HINGE -> "Hip Hinge"
        LiftCategory.QUAD_ISO -> "Quad Isolation"
        LiftCategory.HAMSTRING_ISO -> "Hamstring Isolation"
        LiftCategory.CALVES -> "Calves"
        LiftCategory.HORIZONTAL_PUSH -> "Horizontal Push"
        LiftCategory.VERTICAL_PULL -> "Vertical Pull"
        LiftCategory.HORIZONTAL_PULL -> "Horizontal Pull"
        LiftCategory.CHEST_ISO -> "Chest Isolation"
        LiftCategory.TRICEP_ISO -> "Tricep Isolation"
        LiftCategory.DELT_ISO -> "Deltoid Isolation"
        LiftCategory.TRAP_ISO -> "Trap Isolation"
        LiftCategory.FOREARM_ISO -> "Forearm Isolation"
        LiftCategory.GLUTE_ISO -> "Glute Isolation"
        LiftCategory.AB_ISO -> "Abs"
        LiftCategory.INCLINE_PUSH -> "Incline Push"
        LiftCategory.VERTICAL_PUSH -> "Vertical Push"
    }
}

class LiftCategoryDeserializer : JsonDeserializer<LiftCategory> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LiftCategory {
        val intValue = json?.asInt
        return LiftCategory.values().find { it.ordinal == intValue } ?: throw Exception("$intValue is not a LiftCategory.")
    }
}
