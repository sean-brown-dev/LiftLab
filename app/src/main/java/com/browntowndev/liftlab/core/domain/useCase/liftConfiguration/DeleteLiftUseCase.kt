package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class DeleteLiftUseCase(
    private val liftsRepository: LiftsRepository,
) {
    suspend operator fun invoke(lift: Lift) {
        liftsRepository.delete(lift)
    }
}