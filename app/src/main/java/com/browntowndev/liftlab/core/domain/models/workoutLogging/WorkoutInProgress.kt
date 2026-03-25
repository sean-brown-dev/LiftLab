package com.browntowndev.liftlab.core.domain.models.workoutLogging

import java.util.Date

data class WorkoutInProgress(
    val workoutId: Long,
    val startTime: Date,
)
