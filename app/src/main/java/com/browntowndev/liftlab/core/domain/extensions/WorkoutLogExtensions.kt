package com.browntowndev.liftlab.core.domain.extensions

import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import java.util.Date


/**
 * Filters a list of WorkoutLogEntry to include only those within the given date range.
 */
fun List<WorkoutLogEntry>.filterByDateRange(dateRange: Pair<Date, Date>): List<WorkoutLogEntry> {
    return this.filter { workoutLog ->
        dateRange.first <= workoutLog.date && workoutLog.date <= dateRange.second
    }
}