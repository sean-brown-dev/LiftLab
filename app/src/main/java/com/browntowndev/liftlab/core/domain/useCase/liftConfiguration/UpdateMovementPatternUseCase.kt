package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeUtils
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateMovementPatternUseCase(
    private val liftRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    /**
     * Updates the movement pattern of a lift. If the lift does not exists
     * it is not persisted.
     *
     * @param lift The lift to update.
     * @param movementPattern The new movement pattern to set.
     * @return The updated lift.
     */
    suspend operator fun invoke(lift: Lift, movementPattern: MovementPattern): Lift = transactionScope.executeWithResult {
        val volumeTypes = VolumeTypeUtils.getDefaultVolumeTypes(movementPattern)
        val secondaryVolumeTypes = VolumeTypeUtils.getDefaultSecondaryVolumeTypes(movementPattern)
        val updatedLift = lift.copy(
            movementPattern = movementPattern,
            volumeTypesBitmask = volumeTypes.sumOf { it.bitMask },
            secondaryVolumeTypesBitmask = secondaryVolumeTypes?.sumOf { it.bitMask }
        )

        if (lift.id > 0L) {
            liftRepository.update(updatedLift)
        }

        updatedLift
    }
}