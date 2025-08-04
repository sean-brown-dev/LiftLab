package com.browntowndev.liftlab.core.domain.extensions

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
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



fun List<WorkoutLogEntry>.toFilterOptions(): Map<Long, String> {
    return this.associate {
        it.historicalWorkoutNameId to it.workoutName
    }
}

fun SetResult.toSetLogEntry(
    liftName: String,
    liftMovementPattern: MovementPattern,
    progressionScheme: ProgressionScheme,
    workoutLogEntryId: Long,
    repRangeTop: Int?,
    repRangeBottom: Int?,
    rpeTarget: Float,
    weightRecommendation: Float?,
    mesoCycle: Int,
    microCycle: Int,
): SetLogEntry = SetLogEntry(
        id = this.id,
        workoutLogEntryId = workoutLogEntryId,
        liftId = this.liftId,
        liftName = liftName,
        liftMovementPattern = liftMovementPattern,
        progressionScheme = progressionScheme,
        setType = this.setType,
        liftPosition = this.liftPosition,
        setPosition = this.setPosition,
        myoRepSetPosition = (this as? MyoRepSetResult)?.myoRepSetPosition,
        repRangeTop = repRangeTop,
        repRangeBottom = repRangeBottom,
        rpeTarget = rpeTarget,
        weightRecommendation = weightRecommendation,
        weight = this.weight,
        reps = this.reps,
        rpe = this.rpe,
        mesoCycle = mesoCycle,
        microCycle = microCycle,
        isDeload = this.isDeload,
    )