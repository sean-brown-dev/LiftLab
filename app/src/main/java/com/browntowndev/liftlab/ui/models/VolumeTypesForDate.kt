package com.browntowndev.liftlab.ui.models

import java.time.LocalDate

data class VolumeTypesForDate(
    val date: LocalDate,
    val workingSetVolume: Int,
    val relativeVolume: Float,
)
