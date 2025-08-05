package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class ReplaceWorkoutLiftUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutLiftId: Long, replacementLiftId: Long) = transactionScope.execute {
        workoutLiftsRepository.updateLiftId(workoutLiftId = workoutLiftId, newLiftId = replacementLiftId)
    }
}