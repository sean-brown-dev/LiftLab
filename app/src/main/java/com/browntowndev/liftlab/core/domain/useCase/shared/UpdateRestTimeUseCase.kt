package com.browntowndev.liftlab.core.domain.useCase.shared

import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import kotlin.time.Duration

class UpdateRestTimeUseCase(
    private val liftsRepository: LiftsRepository,
) {
    suspend operator fun invoke(liftId: Long, enabled: Boolean, restTime: Duration?) {
        liftsRepository.updateRestTime(liftId, enabled,restTime)
    }
}