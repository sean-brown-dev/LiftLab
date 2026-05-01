package com.browntowndev.liftlab.ui.mapping

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LiftCompletionSummary
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutCompletionSummary
import com.browntowndev.liftlab.ui.models.workout.LiftCompletionSummaryUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutCompletionSummaryUiModel

fun WorkoutCompletionSummary.toUiModel() =
    WorkoutCompletionSummaryUiModel(
        workoutName = workoutName,
        // ⚡ Bolt: Replaced .map { ... } with .fastMap { ... } to prevent unnecessary iterator allocation during mapping
        liftCompletionSummaries = liftCompletionSummaries.fastMap { it.toUiModel() },
        endTime = endTime,
    )

fun WorkoutCompletionSummaryUiModel.toDomainModel() =
    WorkoutCompletionSummary(
        workoutName = workoutName,
        // ⚡ Bolt: Replaced .map { ... } with .fastMap { ... } to prevent unnecessary iterator allocation during mapping
        liftCompletionSummaries = liftCompletionSummaries.fastMap { it.toDomainModel() },
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

fun LiftCompletionSummaryUiModel.toDomainModel() =
    LiftCompletionSummary(
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
