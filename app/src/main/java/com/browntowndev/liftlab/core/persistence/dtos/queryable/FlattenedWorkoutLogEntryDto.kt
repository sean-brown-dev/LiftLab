package com.browntowndev.liftlab.core.persistence.dtos.queryable

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import java.util.Date

data class FlattenedWorkoutLogEntryDto(
    val id: Long,
    val historicalWorkoutNameId: Long,
    val programId: Long,
    val workoutId: Long,
    val setLogEntryId: Long,
    val programName: String,
    val workoutName: String,
    val programWorkoutCount: Int,
    val programDeloadWeek: Int,
    val date: Date,
    val durationInMillis: Long,
    val liftId: Long,
    val liftName: String,
    val liftMovementPattern: MovementPattern,
    val progressionScheme: ProgressionScheme,
    val setType: SetType,
    val liftPosition: Int,
    val setPosition: Int,
    val myoRepSetPosition: Int?,
    val repRangeTop: Int?,
    val repRangeBottom: Int?,
    val rpeTarget: Float,
    val weightRecommendation: Float?,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val mesoCycle: Int,
    val microCycle: Int,
    val microcyclePosition: Int,
    val setMatching: Boolean? = null,
    val maxSets: Int? = null,
    val repFloor: Int? = null,
    val dropPercentage: Float? = null,
    val isDeload: Boolean,
)