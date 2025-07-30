package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository

class DeleteSetResultByIdUseCase(
    private val setResultsRepository: PreviousSetResultsRepository,
) {
    suspend operator fun invoke(id: Long) {
        setResultsRepository.deleteById(id)
    }
}