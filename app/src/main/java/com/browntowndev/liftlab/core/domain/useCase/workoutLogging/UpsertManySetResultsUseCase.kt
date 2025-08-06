package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository

class UpsertManySetResultsUseCase(
    private val setResultsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(setResults: List<SetResult>): List<Long> = transactionScope.executeWithResult {
        setResultsRepository.upsertMany(setResults)
    }
}