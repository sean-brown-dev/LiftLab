package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.ui.models.workout.ProgramUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel
import com.browntowndev.liftlab.ui.models.workout.getVolumeTypeLabels

@Stable
data class LabState(
    val allPrograms: List<ProgramUiModel> = listOf(),
    val program: ProgramUiModel? = null,
    val idOfProgramToDelete: Long? = null,
    val isCreatingProgram: Boolean = false,
    val isEditingProgramName: Boolean = false,
    val isDeletingProgram: Boolean = false,
    val originalWorkoutName: String? = null,
    val workoutIdToRename: Long? = null,
    val workoutToDelete: WorkoutUiModel? = null,
    val isReordering: Boolean = false,
    val isManagingPrograms: Boolean = false,
    val isEditingDeloadWeek: Boolean = false,
) {
    val originalProgramName: String = program?.name ?: ""

    val combinedVolumeTypes: List<CharSequence> by lazy {
        this.program?.getVolumeTypeLabels(VolumeTypeImpact.COMBINED) ?: listOf()
    }

    val primaryVolumeTypes: List<CharSequence> by lazy {
        this.program?.getVolumeTypeLabels(VolumeTypeImpact.PRIMARY) ?: listOf()
    }

    val secondaryVolumeTypes: List<CharSequence> by lazy {
        this.program?.getVolumeTypeLabels(VolumeTypeImpact.SECONDARY) ?: listOf()
    }

    val nameOfProgramToDelete: String? by lazy {
        if (idOfProgramToDelete == program?.id) {
            program?.name
        } else {
            allPrograms.find { it.id == idOfProgramToDelete }?.name
        }
    }
}