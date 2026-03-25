package com.browntowndev.liftlab.core.domain.models.workout

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import java.util.Date

data class LiftWithHistoryState(
    val lift: Lift,
    val workoutLogEntries: List<WorkoutLogEntry> = emptyList(),
    val maxVolume: Pair<Date, Float>? = null,
    val maxWeight: Pair<Date, Float>? = null,
    val topTenPerformances: List<Pair<Date, SetLogEntry>> = emptyList(),
    val totalReps: Int = 0,
    val totalVolume: Float = 0f,
)
