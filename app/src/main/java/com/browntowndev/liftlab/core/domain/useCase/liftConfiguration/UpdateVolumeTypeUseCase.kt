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
    suspend operator fun invoke(
        lift: Lift,
        index: Int,
        newVolumeType: VolumeType,
        volumeTypeCategory: VolumeTypeCategory
    ) = transactionScope.execute {
        val newVolumeTypeBitmask = lift.volumeTypesBitmask.toVolumeTypes()
            .toMutableList()
            .apply {
                if (index >= size) throw IllegalArgumentException("Index for updating volume type is out of bounds")
                this[index] = newVolumeType
            }.sumOf {
                it.bitMask
            }

        val liftToUpdate = if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) lift.copy(volumeTypesBitmask = newVolumeTypeBitmask)
            else lift.copy(secondaryVolumeTypesBitmask = newVolumeTypeBitmask)
        liftsRepository.update(liftToUpdate)
    }
}