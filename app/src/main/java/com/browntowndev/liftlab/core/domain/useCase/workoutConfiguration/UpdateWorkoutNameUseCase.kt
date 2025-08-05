package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class UpdateWorkoutNameUseCase(
    private val workoutsRepository: WorkoutsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutId: Long, newName: String) = transactionScope.execute {
        workoutsRepository.updateName(workoutId, newName)
    }
}