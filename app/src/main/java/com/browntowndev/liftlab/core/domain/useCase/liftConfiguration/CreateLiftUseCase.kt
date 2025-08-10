package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class CreateLiftUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(lift: Lift) = transactionScope.execute {
        val liftToCreate = if (lift.name.isEmpty()) lift.copy(name = "New Lift") else lift
        liftsRepository.insert(liftToCreate)
    }
}