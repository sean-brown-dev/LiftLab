package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository

class DeleteCustomLiftSetByPositionUseCase(
    private val customSetsRepository: CustomLiftSetsRepository,
) {
    suspend operator fun invoke(workoutLiftId: Long, position: Int) {
        customSetsRepository.deleteByPosition(workoutLiftId, position)
    }
}