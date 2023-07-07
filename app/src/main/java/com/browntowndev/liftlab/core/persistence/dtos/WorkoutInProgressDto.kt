package com.browntowndev.liftlab.core.persistence.dtos

import java.util.Date

data class WorkoutInProgressDto (
    val workoutId: Long,
    val startTime: Date,
)
