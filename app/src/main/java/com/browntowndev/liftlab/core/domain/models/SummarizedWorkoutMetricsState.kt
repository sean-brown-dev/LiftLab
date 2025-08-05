package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

data class SummarizedWorkoutMetricsState(
    val dateOrderedWorkoutLogsWithPersonalRecords: List<WorkoutLogEntry>,
    val topSets: Map<Long, Map<Long, Pair<Int, SetLogEntry>>>
)
