package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertExistingSetResultUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
): BaseUpsertSetLogEntryUseCase() {
    suspend operator fun invoke(
        workoutLogEntryId: Long,
        setResult: SetResult,
        loggingWorkoutLift: LoggingWorkoutLift,
    ): Long = transactionScope.executeWithResult {
        val setLogEntry = getSetLogEntryFromSetResult(loggingWorkoutLift, setResult, workoutLogEntryId)
        setLogEntryRepository.upsert(setLogEntry)
    }
}