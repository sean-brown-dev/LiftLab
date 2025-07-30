package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository

class InsertRestTimerInProgressUseCase(
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
) {
    suspend operator fun invoke(restTime: Long) {
        restTimerInProgressRepository.insert(restTime)
    }
}