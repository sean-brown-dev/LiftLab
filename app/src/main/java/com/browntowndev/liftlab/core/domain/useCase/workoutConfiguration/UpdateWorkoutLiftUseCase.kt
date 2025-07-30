package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class UpdateWorkoutLiftUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
) {
    suspend operator fun invoke(workoutLift: GenericWorkoutLift) {
        workoutLiftsRepository.update(workoutLift)
    }
}