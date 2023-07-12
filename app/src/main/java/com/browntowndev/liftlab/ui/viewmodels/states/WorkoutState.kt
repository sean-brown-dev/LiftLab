package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import androidx.lifecycle.LiveData
import com.browntowndev.liftlab.core.common.getVolumeTypeLabels
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import java.util.Date

@Stable
data class WorkoutState(
    val programMetadata: ActiveProgramMetadataDto? = null,
    val workout: LoggingWorkoutDto? = null,
    val workoutFlow: LiveData<LoggingWorkoutDto>? = null,
    val inProgressWorkout: WorkoutInProgressDto? = null,
    val workoutLogVisible: Boolean = false,
    val restTimerStartedAt: Date? = null,
    val restTime: Long = 0L,
) {
    val volumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels() ?: listOf()
    }

    val inProgress by lazy {
        inProgressWorkout != null
    }

    val startTime by lazy {
        inProgressWorkout?.startTime
    }
}
