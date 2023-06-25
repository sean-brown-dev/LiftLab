package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto

data class WorkoutBuilderState (
    val workout: WorkoutDto? = null,
    val programDeloadWeek: Int? = null,
    val isEditingName: Boolean = false,
    val isReordering: Boolean = false,
    val workoutLiftIdToDelete: Long? = null,
    val changedSetTypeState: ChangedSetTypeState? = null,
    val pickerState: PickerState? = null,
    val detailExpansionStates: HashMap<Long, HashSet<Int>> = hashMapOf(),
) {
    val movementPatternOfDeletingWorkoutLift by lazy {
        workout?.let {
            it.lifts.find { lift -> lift.id == this.workoutLiftIdToDelete }?.liftMovementPattern?.displayName()
        }
    }
}

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