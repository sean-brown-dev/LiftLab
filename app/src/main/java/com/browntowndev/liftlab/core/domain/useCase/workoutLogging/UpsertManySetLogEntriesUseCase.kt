package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertManySetLogEntriesUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(setLogEntries: List<SetLogEntry>): List<Long>  = transactionScope.executeWithResult {
        setLogEntryRepository.upsertMany(setLogEntries)
    }
}