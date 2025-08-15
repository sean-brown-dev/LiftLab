package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository

class DeleteWorkoutLogEntryUseCase(
    private val workoutLogRepository: WorkoutLogRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutLogEntryId: Long) = transactionScope.execute {
        workoutLogRepository.deleteById(workoutLogEntryId)
    }
}