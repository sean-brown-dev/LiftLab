package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class DeleteSetLogEntryByIdUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(id: Long) = transactionScope.execute {
        setLogEntryRepository.deleteById(id)
    }
}