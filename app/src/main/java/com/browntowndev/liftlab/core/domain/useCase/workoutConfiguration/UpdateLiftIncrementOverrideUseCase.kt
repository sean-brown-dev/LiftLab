package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateLiftIncrementOverrideUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(liftId: Long, incrementOverride: Float?) = transactionScope.execute {
        liftsRepository.updateIncrementOverride(liftId, incrementOverride)
    }
}