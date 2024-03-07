package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.common.getVolumeTypeLabels
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import java.util.Date

@Stable
data class WorkoutState(
    val initialized: Boolean = false,
    val programMetadata: ActiveProgramMetadataDto? = null,
    val workout: LoggingWorkoutDto? = null,
    val inProgressWorkout: WorkoutInProgressDto? = null,
    val workoutLogVisible: Boolean = false,
    val isReordering: Boolean = false,
    val restTimerStartedAt: Date? = null,
    val restTime: Long = 0L,
    val completedMyoRepSets: Boolean = false,
) {
    val combinedVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpact.COMBINED) ?: listOf()
    }
    val primaryVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpact.PRIMARY) ?: listOf()
    }
    val secondaryVolumeTypes: List<CharSequence> by lazy {
        this.workout?.getVolumeTypeLabels(VolumeTypeImpact.SECONDARY) ?: listOf()
    }

    val inProgress by lazy {
        inProgressWorkout != null
    }

    val startTime by lazy {
        inProgressWorkout?.startTime
    }

    val setsByPositions by lazy {
        this.workout?.lifts?.associate { lift ->
            lift.position to
                    lift.sets.associateBy { it.position }
        }
    }
}
