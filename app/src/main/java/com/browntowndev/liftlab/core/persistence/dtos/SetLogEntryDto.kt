package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.SetType

data class SetLogEntryDto(
    val liftId: Long,
    val liftName: String,
    val setType: SetType,
    val setPosition: Int,
    val myoRepSetPosition: Int? = null,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val mesoCycle: Int,
    val microCycle: Int,
    val isPersonalRecord: Boolean = false,
)
