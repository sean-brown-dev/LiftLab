package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypes
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateVolumeTypeUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    /**
     * Updates a lift's volume type. If the lift does not exist
     * it is not persisted.
     *
     * @param lift The lift to update
     * @param index The index of the volume type to update
     * @param newVolumeType The new volume type to set
     * @param volumeTypeCategory The category of the volume type to update
     * @return The updated lift
     */
    suspend operator fun invoke(
        lift: Lift,
        index: Int,
        newVolumeType: VolumeType,
        volumeTypeCategory: VolumeTypeCategory
    ): Lift = transactionScope.execute {
        val existingVolumeTypeBitmask = if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) lift.volumeTypesBitmask
            else lift.secondaryVolumeTypesBitmask ?: throw IllegalArgumentException("No secondary volume types found")

        val newVolumeTypeBitmask = existingVolumeTypeBitmask.toVolumeTypes()
            .toMutableList()
            .apply {
                if (index >= size) throw IllegalArgumentException("Index for updating volume type is out of bounds")
                this[index] = newVolumeType
            }.sumOf {
                it.bitMask
            }

        val liftToUpdate = if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) lift.copy(volumeTypesBitmask = newVolumeTypeBitmask)
            else lift.copy(secondaryVolumeTypesBitmask = newVolumeTypeBitmask)

        if (lift.id > 0L) {
            liftsRepository.update(liftToUpdate)
        }

        liftToUpdate
    }
}