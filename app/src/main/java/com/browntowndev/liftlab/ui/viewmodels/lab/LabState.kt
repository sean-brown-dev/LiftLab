package com.browntowndev.liftlab.ui.viewmodels.lab

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection
import com.browntowndev.liftlab.ui.extensions.getVolumeTypeLabels
import com.browntowndev.liftlab.ui.models.workout.ProgramUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel

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
        this.program?.getVolumeTypeLabels(VolumeTypeImpactSelection.COMBINED) ?: listOf()
    }

    val primaryVolumeTypes: List<CharSequence> by lazy {
        this.program?.getVolumeTypeLabels(VolumeTypeImpactSelection.PRIMARY) ?: listOf()
    }

    val secondaryVolumeTypes: List<CharSequence> by lazy {
        this.program?.getVolumeTypeLabels(VolumeTypeImpactSelection.SECONDARY) ?: listOf()
    }

    val nameOfProgramToDelete: String? by lazy {
        if (idOfProgramToDelete == program?.id) {
            program?.name
        } else {
            allPrograms.find { it.id == idOfProgramToDelete }?.name
        }
    }
}