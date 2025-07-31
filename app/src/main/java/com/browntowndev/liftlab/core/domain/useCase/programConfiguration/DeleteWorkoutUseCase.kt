package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class DeleteWorkoutUseCase(
    private val workoutsRepository: WorkoutsRepository,
) {
    suspend operator fun invoke(workout: Workout) {
        workoutsRepository.delete(workout)
    }
}