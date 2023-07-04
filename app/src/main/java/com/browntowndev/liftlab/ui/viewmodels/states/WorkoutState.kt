package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.getVolumeTypeLabels
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto

@Stable
data class WorkoutState(
    val program: ProgramDto? = null,
    val workout: WorkoutDto? = null,
    val inProgress: Boolean = false,
    val workoutLogVisible: Boolean = false,
) {
    val volumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels() ?: listOf()
    }
}
