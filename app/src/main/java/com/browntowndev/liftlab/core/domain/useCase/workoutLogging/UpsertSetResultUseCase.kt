package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository

class UpsertSetResultUseCase(
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(setResult: SetResult): Long = transactionScope.executeWithResult {
        liveWorkoutCompletedSetsRepository.upsert(setResult)
    }
}