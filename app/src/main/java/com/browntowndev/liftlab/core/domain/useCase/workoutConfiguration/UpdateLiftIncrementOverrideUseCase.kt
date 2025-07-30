package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateLiftIncrementOverrideUseCase(
    private val liftsRepository: LiftsRepository,
) {
    suspend operator fun invoke(liftId: Long, incrementOverride: Float?) {
        liftsRepository.updateIncrementOverride(liftId, incrementOverride)
    }
}