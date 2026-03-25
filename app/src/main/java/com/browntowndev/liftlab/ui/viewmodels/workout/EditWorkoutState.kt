package com.browntowndev.liftlab.ui.viewmodels.workout

import androidx.compose.runtime.Stable

@Stable
data class EditWorkoutState(
    val duration: String = "00:00:00",
)