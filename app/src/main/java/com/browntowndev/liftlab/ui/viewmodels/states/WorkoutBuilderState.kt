package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto

data class WorkoutBuilderState (
    val workout: WorkoutDto? = null,
    val isEditingName: Boolean = false,
    val changedSetTypeState: ChangedSetTypeState? = null,
    val pickerState: PickerState? = null,
    val detailExpansionStates: HashMap<Long, HashSet<Int>> = hashMapOf(),
)

data class ChangedSetTypeState(
    val workoutLiftId: Long,
    val position: Int,
    val isExpanded: Boolean,
)

data class PickerState(
    val workoutLiftId: Long,
    val position: Int? = null,
    val type: PickerType,
)

enum class PickerType {
    Rpe,
    Percentage,
}