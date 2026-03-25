package com.browntowndev.liftlab.ui.models.workoutLogging

import java.util.Date

data class WorkoutLogEntryUiModel(
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
    val setLogEntries: List<SetLogEntryUiModel>,
)
