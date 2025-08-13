package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypes
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
        transactionScope.execute {
            val liftToUpdate =
                if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) {
                    val currentVolumeTypes = lift.volumeTypesBitmask.toVolumeTypes()
                    if (currentVolumeTypes.size == 1) throw IllegalArgumentException("Cannot remove the only remaining volume type")
                    if (!currentVolumeTypes.contains(volumeTypeToRemove)) throw IllegalArgumentException("Volume type not present")
                    val newVolumeType = lift.volumeTypesBitmask - volumeTypeToRemove.bitMask

                    lift.copy(volumeTypesBitmask = newVolumeType)
                }
                else {
                    val currentVolumeTypes = lift.volumeTypesBitmask.toVolumeTypes()
                    if (!currentVolumeTypes.contains(volumeTypeToRemove)) throw IllegalArgumentException("Volume type not present")
                    val newVolumeType = (lift.secondaryVolumeTypesBitmask ?: 0) - volumeTypeToRemove.bitMask

                    lift.copy(secondaryVolumeTypesBitmask = newVolumeType)
                }

            if (lift.id > 0L) {
                liftsRepository.update(liftToUpdate)
            }

            liftToUpdate
        }
}