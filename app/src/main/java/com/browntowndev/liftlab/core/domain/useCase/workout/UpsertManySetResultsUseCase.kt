package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository

class UpsertManySetResultsUseCase(
    private val setResultsRepository: PreviousSetResultsRepository
) {
    suspend operator fun invoke(setResults: List<SetResult>): List<Long> =
        setResultsRepository.upsertMany(setResults)
}