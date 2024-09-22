package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable


@Stable
data class PickerState(
    val workoutLiftId: Long? = null,
    val setPosition: Int? = null,
    val myoRepSetPosition: Int? = null,
    val currentRpe: Float? = null,
    val currentPercentage: Float? = null,
    val type: PickerType? = null,
)
