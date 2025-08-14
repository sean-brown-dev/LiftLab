package com.browntowndev.liftlab.ui.viewmodels.states.workout

import androidx.compose.runtime.Stable
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection
import com.browntowndev.liftlab.ui.extensions.getVolumeTypeLabels
import com.browntowndev.liftlab.ui.models.metrics.PersonalRecordUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutCompletionSummaryUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.SetResultUiModel

@Stable
data class WorkoutState(
    val initialized: Boolean = false,
    val programMetadata: ActiveProgramMetadataUiModel? = null,
    val workout: LoggingWorkoutUiModel? = null,
    val personalRecords: Map<Long, PersonalRecordUiModel> = emptyMap(),
    val inProgressWorkout: WorkoutInProgressUiModel? = null,
    val completedSets: List<SetResultUiModel> = emptyList(),
    val workoutLogVisible: Boolean = false,
    val isReordering: Boolean = false,
    val isCompletionSummaryVisible: Boolean = false,
    val isConfirmCancelWorkoutDialogShown: Boolean = false,
    val isDeloadPromptDialogShown: Boolean = false,
    val workoutCompletionSummary: WorkoutCompletionSummaryUiModel? = null,
) {
    val isDeloadWeek by lazy {
        this.programMetadata != null &&
                this.programMetadata.deloadWeek == (this.programMetadata.currentMicrocycle + 1)
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

    val inProgress by lazy {
        inProgressWorkout != null
    }

    val startTime by lazy {
        inProgressWorkout?.startTime
    }

}