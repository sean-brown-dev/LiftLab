package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class DeleteWorkoutLiftUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutLift: GenericWorkoutLift) = transactionScope.execute {
        workoutLiftsRepository.delete(workoutLift)
        customLiftSetsRepository.deleteAllForLift(workoutLift.id)
    }
}