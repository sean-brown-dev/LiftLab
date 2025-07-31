package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertSetLogEntryUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
) {
    suspend operator fun invoke(setLogEntry: SetLogEntry): Long {
        return setLogEntryRepository.upsert(setLogEntry)
    }
}