package com.browntowndev.liftlab.ui.models

import java.math.BigDecimal

class WorkoutCompletionSummary(val liftCompletions: List<LiftCompletionSummary>) {
    val percentageComplete: Float by lazy {
        val completedSets: Int = liftCompletions.sumOf { it.setsCompleted }
        val totalSets: Int = liftCompletions.sumOf { it.totalSets }

        if (totalSets > 0) {
            (completedSets.toFloat() / totalSets) * 100
        } else 0f
    }

    val totalIncompleteLifts: Int by lazy {
        liftCompletions.count { it.isIncomplete }
    }
}