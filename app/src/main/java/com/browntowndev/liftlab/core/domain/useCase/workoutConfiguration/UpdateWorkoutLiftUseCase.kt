package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class UpdateWorkoutLiftUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutLift: GenericWorkoutLift) = transactionScope.execute {
        workoutLiftsRepository.update(workoutLift)
    }
}