package com.browntowndev.liftlab.core.domain.models.workoutLogging

import java.util.Date

data class WorkoutLogEntry(
    val id: Long,
    val historicalWorkoutNameId: Long,
    val programWorkoutCount: Int,
    val programDeloadWeek: Int,
    val programName: String,
    val workoutName: String,
    val programId: Long,
    val workoutId: Long,
    val mesocycle: Int,
    val microcycle: Int,
    val microcyclePosition: Int,
    val date: Date,
    val durationInMillis: Long,
    val setResults: List<SetLogEntry>,
)
