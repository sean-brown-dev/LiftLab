package com.browntowndev.liftlab.core.domain.models.workoutLogging

import com.browntowndev.liftlab.core.common.Utils
import java.util.Date

data class WorkoutCompletionSummary(
    val workoutName: String,
    val liftCompletionSummaries: List<LiftCompletionSummary>,
    val endTime: Date = Utils.General.Companion.getCurrentDate(),
)