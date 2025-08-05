package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import kotlin.time.Duration

class UpdateRestTimeUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(liftId: Long, enabled: Boolean, restTime: Duration?) = transactionScope.execute {
        liftsRepository.updateRestTime(liftId, enabled,restTime)
    }
}