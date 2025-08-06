package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata

data class CompletedWorkoutState(
    val workout: LoggingWorkout? = null,
    val duration: String? = null,
    val completedSetsFromLog: List<SetResult> = emptyList(),
    val programMetadata: ActiveProgramMetadata? = null,
)
