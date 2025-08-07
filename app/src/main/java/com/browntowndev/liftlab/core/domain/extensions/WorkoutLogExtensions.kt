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

fun LoggingWorkoutLift.findSet(setPosition: Int, myoRepSetPosition: Int?): GenericLoggingSet? {
    // Will be null for newly added set results from the edit workout screen
    val set = sets.fastFirstOrNull { set ->
        set.position == setPosition && (set as? LoggingMyoRepSet)?.myoRepSetPosition == myoRepSetPosition
    }

    return set
}
