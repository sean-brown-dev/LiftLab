package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository

class InsertRestTimerInProgressUseCase(
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(restTimeInMillis: Long) = transactionScope.execute  {
        restTimerInProgressRepository.upsert(
            startTimeInMillis = getCurrentDate().time,
            restTimeInMillis = restTimeInMillis)
    }
}