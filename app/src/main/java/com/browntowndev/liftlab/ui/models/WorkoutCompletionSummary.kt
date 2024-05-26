package com.browntowndev.liftlab.ui.models

import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import java.util.Date

class WorkoutCompletionSummary(
    val workoutName: String,
    val liftCompletionSummaries: List<LiftCompletionSummary>,
    val endTime: Date = getCurrentDate(),
) {
    val percentageComplete: Float by lazy {
        val completedSets: Int = liftCompletionSummaries.sumOf { it.setsCompleted }
        val totalSets: Int = liftCompletionSummaries.sumOf { it.totalSets }

        if (totalSets > 0) {
            (completedSets.toFloat() / totalSets) * 100
        } else 0f
    }

    val totalIncompleteLifts: Int by lazy {
        liftCompletionSummaries.count { it.isIncomplete }
    }

    val personalRecordCount by lazy {
        liftCompletionSummaries.count { it.isNewPersonalRecord }
    }
}