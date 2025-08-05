package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository

class InsertRestTimerInProgressUseCase(
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(restTime: Long) = transactionScope.execute  {
        restTimerInProgressRepository.insert(restTime)
    }
}