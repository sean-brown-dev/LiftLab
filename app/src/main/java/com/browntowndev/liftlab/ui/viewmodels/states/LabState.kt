package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.common.getVolumeTypeLabels
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto

@Stable
data class LabState(
    val program: ProgramDto? = null,
    val isCreatingProgram: Boolean = false,
    val isEditingProgramName: Boolean = false,
    val isDeletingProgram: Boolean = false,
    val originalWorkoutName: String? = null,
    val workoutIdToRename: Long? = null,
    val workoutToDelete: WorkoutDto? = null,
    val isReordering: Boolean = false,
    val workoutCreated: Boolean = false,
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
}