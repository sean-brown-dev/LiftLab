package com.browntowndev.liftlab.core.domain.models.metrics

import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

data class SummarizedWorkoutMetricsState(
    val dateOrderedWorkoutLogsWithPersonalRecords: List<WorkoutLogEntry>,
    val topSets: AllWorkoutTopSets
)