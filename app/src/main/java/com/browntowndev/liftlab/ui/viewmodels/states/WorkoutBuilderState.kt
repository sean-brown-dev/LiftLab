package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.getVolumeTypeLabels
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto

@Stable
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

    val combinedVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpact.COMBINED) ?: listOf()
    }
    val primaryVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpact.PRIMARY) ?: listOf()
    }
    val secondaryVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpact.SECONDARY) ?: listOf()
    }
}

@Stable
data class ChangedSetTypeState(
    val workoutLiftId: Long,
    val position: Int,
    val isExpanded: Boolean,
)

enum class PickerType {
    Rpe,
    Percentage,
}