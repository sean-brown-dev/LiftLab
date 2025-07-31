package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class DeleteSetLogEntryByIdUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
) {
    suspend operator fun invoke(id: Long) {
        setLogEntryRepository.deleteById(id)
    }
}