package com.browntowndev.liftlab.ui.mapping

import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutInProgress
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel

fun WorkoutInProgress.toUiModel(): WorkoutInProgressUiModel =
    WorkoutInProgressUiModel(
        startTime = this.startTime,
    )

fun WorkoutInProgressUiModel.toDomainModel(workoutId: Long) =
    WorkoutInProgress(
        workoutId = workoutId,
        startTime = this.startTime,
    )
