package com.browntowndev.liftlab.core.domain.extensions

import androidx.compose.ui.util.fastFirstOrNull
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
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
        isDeload = this.isDeload,
    )

fun SetResult.copyCompletionDataFromSetLogEntry(other: SetLogEntry): SetResult =
    this.copyBase(
        reps = other.reps,
        weight = other.weight,
        rpe = other.rpe,
    )

fun List<LoggingWorkoutLift>.findSet(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?): GenericLoggingSet? {
    if (liftPosition >= this.size) throw Exception("Lift position is out of bounds")
    val lift = this[liftPosition]
    return lift.findSet(setPosition = setPosition, myoRepSetPosition = myoRepSetPosition)
}

fun LoggingWorkoutLift.findSet(setPosition: Int, myoRepSetPosition: Int?): GenericLoggingSet? {
    // Will be null for newly added set results from the edit workout screen
    val set = sets.fastFirstOrNull { set ->
        set.position == setPosition && (set as? LoggingMyoRepSet)?.myoRepSetPosition == myoRepSetPosition
    }

    return set
}