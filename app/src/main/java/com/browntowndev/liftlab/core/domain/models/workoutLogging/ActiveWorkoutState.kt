package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import java.util.Date

data class ActiveWorkoutState(
    val programMetadata: ActiveProgramMetadata? = null,
    val inProgressWorkout: WorkoutInProgress? = null,
    val completedSets: List<SetResult> = emptyList(),
    val workout: LoggingWorkout? = null,
    val personalRecords: Map<Long, PersonalRecord> = emptyMap(),
    val restTimerStartedAt: Date? = null,
    val restTime: Long = 0L,
)