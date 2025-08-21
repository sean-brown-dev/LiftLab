package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository

class RestTimerCompletedUseCase(
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke() = transactionScope.execute {
        restTimerInProgressRepository.delete()
    }
}