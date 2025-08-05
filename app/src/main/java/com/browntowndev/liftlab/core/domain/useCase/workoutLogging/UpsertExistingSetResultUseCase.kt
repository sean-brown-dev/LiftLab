package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertExistingSetResultUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val transactionScope: TransactionScope,
): BaseUpsertSetLogEntryUseCase() {
    suspend operator fun invoke(
        workoutLogEntryId: Long,
        mesoCycle: Int,
        microCycle: Int,
        setResult: SetResult,
        loggingWorkoutLift: LoggingWorkoutLift,
        allSetResults: List<SetResult>,
    ): Long = transactionScope.executeWithResult {
        Log.d("UpsertExistingSetResultUseCase", "upserting set result: $setResult")
        getUpdatedSetResultIfExistsOrNull(allSetResults, setResult)?.let {
            previousSetResultsRepository.update(it)
        }
        val setLogEntry = getSetLogEntryFromSetResult(loggingWorkoutLift, setResult, workoutLogEntryId, mesoCycle, microCycle)
        setLogEntryRepository.upsert(setLogEntry)
    }
}