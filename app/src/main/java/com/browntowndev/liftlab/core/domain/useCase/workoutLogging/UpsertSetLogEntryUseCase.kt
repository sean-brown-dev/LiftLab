package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertSetLogEntryUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(setLogEntry: SetLogEntry): Long = transactionScope.executeWithResult {
        setLogEntryRepository.upsert(setLogEntry)
    }
}