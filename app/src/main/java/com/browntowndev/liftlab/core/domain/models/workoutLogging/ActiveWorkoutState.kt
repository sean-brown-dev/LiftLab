package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata

data class ActiveWorkoutState(
    val programMetadata: ActiveProgramMetadata? = null,
    val inProgressWorkout: WorkoutInProgress? = null,
    val completedSets: List<SetResult> = emptyList(),
    val workout: LoggingWorkout? = null,
    val personalRecords: Map<Long, PersonalRecord> = emptyMap(),
)