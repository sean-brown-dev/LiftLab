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
    suspend operator fun invoke(lift: Lift, newVolumeType: VolumeType, volumeTypeCategory: VolumeTypeCategory) = transactionScope.execute {
        val newVolumeTypeBitmask = lift.volumeTypesBitmask + newVolumeType.bitMask
        val liftToUpdate = if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) lift.copy(volumeTypesBitmask = newVolumeTypeBitmask)
            else lift.copy(secondaryVolumeTypesBitmask = newVolumeTypeBitmask)

        liftsRepository.update(liftToUpdate)
    }
}