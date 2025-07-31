package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertManySetLogEntriesUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
) {
    suspend operator fun invoke(setLogEntries: List<SetLogEntry>): List<Long> {
        return setLogEntryRepository.upsertMany(setLogEntries)
    }
}