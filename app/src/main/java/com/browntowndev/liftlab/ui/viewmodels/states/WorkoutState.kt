package com.browntowndev.liftlab.ui.viewmodels.states

import com.browntowndev.liftlab.core.common.getVolumeTypeLabels
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto

data class WorkoutState(
    val program: ProgramDto? = null,
    val workout: WorkoutDto? = null,
    val inProgress: Boolean = false,
) {
    val volumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels() ?: listOf()
    }
}
