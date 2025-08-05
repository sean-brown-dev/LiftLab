package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateVolumeTypeUseCase(
    private val liftsRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(lift: Lift, newVolumeTypeBitmask: Int?, volumeTypeCategory: VolumeTypeCategory) = transactionScope.execute {
        val liftToUpdate = if (volumeTypeCategory == VolumeTypeCategory.PRIMARY) lift.copy(volumeTypesBitmask = newVolumeTypeBitmask ?: throw IllegalArgumentException("Primary volume type cannot be null"))
            else lift.copy(secondaryVolumeTypesBitmask = newVolumeTypeBitmask)
        liftsRepository.update(liftToUpdate)
    }
}