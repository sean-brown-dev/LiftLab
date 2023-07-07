package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.getVolumeTypeLabels
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutWithProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

@Stable
data class WorkoutState(
    val programMetadata: ActiveProgramMetadataDto? = null,
    val workoutWithProgression: WorkoutWithProgressionDto? = null,
    val inProgress: Boolean = false,
    val workoutLogVisible: Boolean = false,
    val completedSets: Map<String, SetResult> = hashMapOf(),
) {
    val volumeTypes: List<CharSequence> by lazy {
        this.workoutWithProgression?.workout?.getVolumeTypeLabels() ?: listOf()
    }
}
