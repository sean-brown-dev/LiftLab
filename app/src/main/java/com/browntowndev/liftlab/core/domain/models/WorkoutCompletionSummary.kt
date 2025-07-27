package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import java.util.Date

data class WorkoutCompletionSummary(
    val workoutName: String,
    val liftCompletionSummaries: List<LiftCompletionSummary>,
    val endTime: Date = getCurrentDate(),
)
