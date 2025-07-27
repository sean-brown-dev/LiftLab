package com.browntowndev.liftlab.core.domain.models.workout

import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.domain.models.LiftCompletionSummary
import java.util.Date

data class WorkoutCompletionSummary(
    val workoutName: String,
    val liftCompletionSummaries: List<LiftCompletionSummary>,
    val endTime: Date = Utils.General.Companion.getCurrentDate(),
)