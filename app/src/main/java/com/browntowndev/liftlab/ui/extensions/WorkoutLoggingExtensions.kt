package com.browntowndev.liftlab.ui.extensions

import com.browntowndev.liftlab.ui.models.workoutLogging.WorkoutLogEntryUiModel
import java.util.Date


/**
 * Filters a list of WorkoutLogEntry to include only those within the given date range.
 */
fun List<WorkoutLogEntryUiModel>.filterByDateRange(dateRange: Pair<Date, Date>): List<WorkoutLogEntryUiModel> {
    return this.filter { workoutLog ->
        dateRange.first <= workoutLog.date && workoutLog.date <= dateRange.second
    }
}