package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository

class DeleteSetResultByIdUseCase(
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(id: Long): Int  = transactionScope.execute {
        liveWorkoutCompletedSetsRepository.deleteById(id)
    }
}