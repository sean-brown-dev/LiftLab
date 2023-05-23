package com.browntowndev.liftlab.core.common.enums

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

enum class LiftCategory {
    LEG_PUSH_COMPOUND,
    HIP_COMPOUND,
    QUAD_ISO,
    HAMSTRING_ISO,
    CALVES,

    HORIZONTAL_PUSH,
    VERTICAL_PULL,
    HORIZONTAL_PULL,
    CHEST_ISO,
    TRICEP_ISO,
    BICEP_ISO,
    DELTS_ISO,
    UNKNOWN,
}

class LiftCategoryDeserializer : JsonDeserializer<LiftCategory> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LiftCategory {
        val intValue = json?.asInt
        return LiftCategory.values().find { it.ordinal == intValue } ?: LiftCategory.UNKNOWN
    }
}
