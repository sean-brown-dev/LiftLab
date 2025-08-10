package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class RemoveVolumeTypeUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    /**
     * Removes a volume type from a lift. If the lift does not exist it is not
     * persisted.
     *
     * @param lift The lift to remove the volume type from
     * @param volumeTypeToRemove The volume type to remove
     * @param volumeTypeCategory The category of the volume type to remove
     * @return The updated lift
     */
    suspend operator fun invoke(
        lift: Lift,
        volumeTypeToRemove: VolumeType,
        volumeTypeCategory: VolumeTypeCategory
    ): Lift =
        transactionScope.executeWithResult {
            val newVolumeTypeBitmask = lift.volumeTypesBitmask - volumeTypeToRemove.bitMask
            val nullableVolumeTypeBitmask = if (newVolumeTypeBitmask == 0) null else newVolumeTypeBitmask
            if (nullableVolumeTypeBitmask == null) throw IllegalArgumentException("Primary volume type cannot be removed")

            val liftToUpdate =
                if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) lift.copy(volumeTypesBitmask = newVolumeTypeBitmask)
                else lift.copy(secondaryVolumeTypesBitmask = nullableVolumeTypeBitmask)

            if (lift.id > 0L) {
                liftsRepository.update(liftToUpdate)
            }

            liftToUpdate
        }
}