package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository

class DeleteCustomLiftSetByPositionUseCase(
    private val customSetsRepository: CustomLiftSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutLiftId: Long, position: Int) = transactionScope.execute {
        customSetsRepository.deleteByPosition(workoutLiftId, position)
    }
}