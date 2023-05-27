package com.browntowndev.liftlab.core.data.dtos

import androidx.room.Embedded
import com.browntowndev.liftlab.core.data.entities.Lift

data class LiftDTO(
    @Embedded
    val lift: Lift
)