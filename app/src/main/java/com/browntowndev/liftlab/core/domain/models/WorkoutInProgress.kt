package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import java.util.Date

data class WorkoutInProgress(
    val workoutId: Long,
    val startTime: Date,
    val completedSets: List<SetResult>,
)
