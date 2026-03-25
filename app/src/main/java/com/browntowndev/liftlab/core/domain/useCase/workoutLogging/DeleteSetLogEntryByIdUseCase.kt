package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class DeleteSetLogEntryByIdUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(id: Long) = transactionScope.execute {
        val deleteCount = setLogEntryRepository.deleteById(id)
        Log.d("DeleteSetLogEntryByIdUseCase", "Delete count: $deleteCount set log ID: $id")
        deleteCount
    }
}