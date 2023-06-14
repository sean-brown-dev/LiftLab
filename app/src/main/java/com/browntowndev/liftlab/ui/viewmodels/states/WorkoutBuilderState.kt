package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto

data class WorkoutBuilderState (
    val workout: WorkoutDto? = null,
    val isEditingName: Boolean = false,
    val rpePickerState: RpePickerState? = null,
)

data class RpePickerState(
    val workoutLiftId: Long,
    val position: Int? = null,
)