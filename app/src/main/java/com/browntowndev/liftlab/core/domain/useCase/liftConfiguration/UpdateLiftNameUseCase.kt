package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateLiftNameUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(lift: Lift, newName: String) = transactionScope.execute {
        liftsRepository.update(lift.copy(name = newName))
    }
}