package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class DeleteWorkoutUseCase(
    private val workoutsRepository: WorkoutsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workout: Workout) = transactionScope.execute {
        workoutsRepository.delete(workout)
    }
}