package com.browntowndev.liftlab.core.persistence.dtos.queryable

import com.browntowndev.liftlab.core.common.enums.SetType
import java.util.Date

data class FlattenedWorkoutLogEntries (
    val historicalWorkoutNameId: Long,
    val programName: String,
    val workoutName: String,
    val date: Date,
    val durationInMillis: Long,
    val setType: SetType,
    val setPosition: Int,
    val myoRepSetPosition: Int? = null,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val mesoCycle: Int,
    val microCycle: Int,
    val missedLpGoals: Int? = null,
)