package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository

class UpsertSetResultUseCase(
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(setResult: SetResult): Long = transactionScope.executeWithResult {
        previousSetResultsRepository.upsert(setResult)
    }
}