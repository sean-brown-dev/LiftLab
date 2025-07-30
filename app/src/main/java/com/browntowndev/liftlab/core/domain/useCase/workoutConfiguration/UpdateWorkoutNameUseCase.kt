package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class UpdateWorkoutNameUseCase(
    private val workoutsRepository: WorkoutsRepository
) {
    suspend operator fun invoke(workoutId: Long, newName: String) {
        workoutsRepository.updateName(workoutId, newName)
    }
}