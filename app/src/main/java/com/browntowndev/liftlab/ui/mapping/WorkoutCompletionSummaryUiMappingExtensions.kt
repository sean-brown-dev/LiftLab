package com.browntowndev.liftlab.ui.mapping

import com.browntowndev.liftlab.core.domain.models.workoutLogging.LiftCompletionSummary
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutCompletionSummary
import com.browntowndev.liftlab.ui.models.workout.LiftCompletionSummaryUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutCompletionSummaryUiModel

object WorkoutCompletionSummaryUiMappingExtensions {
    fun WorkoutCompletionSummary.toUiModel() =
        WorkoutCompletionSummaryUiModel(
            workoutName = workoutName,
            liftCompletionSummaries = liftCompletionSummaries.map { it.toUiModel() },
            endTime = endTime,
        )

    fun LiftCompletionSummary.toUiModel() =
        LiftCompletionSummaryUiModel(
            liftName = liftName,
            liftId = liftId,
            liftPosition = liftPosition,
            setsCompleted = setsCompleted,
            totalSets = totalSets,
            bestSetReps = bestSetReps,
            bestSetWeight = bestSetWeight,
            bestSetRpe = bestSetRpe,
            bestSet1RM = bestSet1RM,
            isNewPersonalRecord = isNewPersonalRecord,
        )
}