package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.domain.models.metrics.AllWorkoutTopSets
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

data class SummarizedWorkoutMetricsState(
    val dateOrderedWorkoutLogsWithPersonalRecords: List<WorkoutLogEntry>,
    val topSets: AllWorkoutTopSets
)
