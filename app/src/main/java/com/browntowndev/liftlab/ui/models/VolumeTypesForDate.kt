package com.browntowndev.liftlab.ui.models

import java.time.LocalDate

data class VolumeTypesForDate(
    val date: LocalDate,
    val repVolume: Int,
    val weightVolume: Float,
)
