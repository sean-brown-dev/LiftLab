package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection
import com.browntowndev.liftlab.core.domain.enums.displayName
import com.browntowndev.liftlab.ui.extensions.getVolumeTypeLabels
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel

@Stable
data class WorkoutBuilderState (
    val workout: WorkoutUiModel? = null,
    val programDeloadWeek: Int? = null,
    val isEditingName: Boolean = false,
    val isReordering: Boolean = false,
    val workoutLiftIdToDelete: Long? = null,
    val pickerState: PickerState? = null,
    val detailExpansionStates: HashMap<Long, HashSet<Int>> = hashMapOf(),
    val workoutLiftStepSizeOptions: Map<Long, Map<Int, List<Int>>> = mapOf(),
) {
    val movementPatternOfDeletingWorkoutLift by lazy {
        workout?.let {
            it.lifts.find { lift -> lift.id == this.workoutLiftIdToDelete }?.liftMovementPattern?.displayName()
        }
    }

    val combinedVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpactSelection.COMBINED) ?: listOf()
    }
    val primaryVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpactSelection.PRIMARY) ?: listOf()
    }
    val secondaryVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpactSelection.SECONDARY) ?: listOf()
    }
}

enum class PickerType {
    Rpe,
    Percentage,
}