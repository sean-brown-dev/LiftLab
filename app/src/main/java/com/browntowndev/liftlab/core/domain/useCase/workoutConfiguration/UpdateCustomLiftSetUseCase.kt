package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository

class UpdateCustomLiftSetUseCase(
    private val customSetsRepository: CustomLiftSetsRepository,
) {
    suspend operator fun invoke(set: GenericLiftSet) {
        customSetsRepository.update(set)

    }
}