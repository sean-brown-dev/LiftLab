package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import java.util.Date

data class WorkoutInProgressDto(
    val workoutId: Long,
    val startTime: Date,
    val completedSets: List<SetResult>,
)
