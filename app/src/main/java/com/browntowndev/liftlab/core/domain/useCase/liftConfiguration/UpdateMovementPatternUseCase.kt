package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeUtils
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateMovementPatternUseCase(private val liftRepository: LiftsRepository) {
    suspend operator fun invoke(lift: Lift, movementPattern: MovementPattern) {
        val volumeTypes = VolumeTypeUtils.getDefaultVolumeTypes(movementPattern)
        val secondaryVolumeTypes = VolumeTypeUtils.getDefaultSecondaryVolumeTypes(movementPattern)
        val updatedLift = lift.copy(
            movementPattern = movementPattern,
            volumeTypesBitmask = volumeTypes.sumOf { it.bitMask },
            secondaryVolumeTypesBitmask = secondaryVolumeTypes?.sumOf { it.bitMask }
        )

        liftRepository.update(updatedLift)
    }
}