package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class AddVolumeTypeUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    /**
     * Adds a volume type to the lift. If the lift does not exist it
     * is not persisted.
     *
     * @param lift The lift to update
     * @param newVolumeType The volume type to add
     * @param volumeTypeCategory The category of the volume type to add
     * @return The updated lift
     */
    suspend operator fun invoke(lift: Lift, newVolumeType: VolumeType, volumeTypeCategory: VolumeTypeCategory): Lift = transactionScope.executeWithResult {
        val newVolumeTypeBitmask = lift.volumeTypesBitmask + newVolumeType.bitMask
        val liftToUpdate = if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) lift.copy(volumeTypesBitmask = newVolumeTypeBitmask)
            else lift.copy(secondaryVolumeTypesBitmask = newVolumeTypeBitmask)

        if (lift.id > 0L) {
            liftsRepository.update(liftToUpdate)
        }

        liftToUpdate
    }
}