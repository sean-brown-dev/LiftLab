package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel

class StartWorkoutUseCase(
    private val workoutInProgressRepository: WorkoutInProgressRepository
) {
    suspend fun start(workoutId: Long) {
        val inProgressWorkout = WorkoutInProgressUiModel(
            startTime = getCurrentDate(),
        )
        workoutInProgressRepository.insert(
            inProgressWorkout.toDomainModel(
                workoutId
            )
        )
    }
}