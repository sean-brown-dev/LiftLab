package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class ReorderWorkoutsUseCase(
    private val workoutsRepository: WorkoutsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workouts: List<Workout>, newOrders: Map<Long, Int>) = transactionScope.execute {
        val updatedWorkouts = workouts.map { workout ->
            val newOrder = newOrders[workout.id]
            if (newOrder != null) {
                workout.copy(position = newOrder)
            } else throw IllegalArgumentException("New position not found for workout: ${workout.id}")
        }

        workoutsRepository.updateMany(updatedWorkouts)
    }
}