package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository

class UpdateCustomLiftSetUseCase(
    private val customSetsRepository: CustomLiftSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(set: GenericLiftSet) = transactionScope.execute {
        customSetsRepository.update(set)
    }
}