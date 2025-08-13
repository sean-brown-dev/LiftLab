package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateLiftNameUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    /**
     * Updates the name of a lift and returns the updated lift.
     * If the lift does not exist, the updated lift is returned but not persisted.
     *
     * @param lift The lift to update.
     * @param newName The new name for the lift.
     * @return The updated lift.
     */
    suspend operator fun invoke(lift: Lift, newName: String): Lift = transactionScope.execute {
        val liftToUpdate = lift.copy(name = newName)

        if (liftToUpdate.id > 0L) {
            liftsRepository.update(liftToUpdate)
        }

        liftToUpdate
    }
}