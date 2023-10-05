package com.browntowndev.liftlab.core.persistence.dtos

import java.util.Date

data class WorkoutLogEntryDto(
    val programName: String,
    val workoutName: String,
    val mesocycle: Int,
    val microcycle: Int,
    val date: Date,
    val durationInMillis: Long,
    val setResults: List<SetLogEntryDto>,
)
