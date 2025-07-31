package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class ReplaceWorkoutLiftUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
) {
    suspend operator fun invoke(workoutLiftId: Long, replacementLiftId: Long) {
        workoutLiftsRepository.updateLiftId(workoutLiftId = workoutLiftId, newLiftId = replacementLiftId)
    }
}