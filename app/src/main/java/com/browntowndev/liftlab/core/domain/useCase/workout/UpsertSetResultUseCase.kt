package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository

class UpsertSetResultUseCase(
    private val previousSetResultsRepository: PreviousSetResultsRepository,
) {
    suspend operator fun invoke(setResult: SetResult): Long =
        previousSetResultsRepository.upsert(setResult)
}