package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertSetLogEntriesFromSetResultsUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val transactionScope: TransactionScope,
): BaseUpsertSetLogEntryUseCase() {
    suspend operator fun invoke(
        workoutLogEntryId: Long,
        mesoCycle: Int,
        microCycle: Int,
        loggingWorkoutLifts: List<LoggingWorkoutLift>,
        allSetResults: List<SetResult>,
        setResults: List<SetResult>,
    ): List<Long>  = transactionScope.executeWithResult {
        setResults.fastMap { setResult ->
            val maybeUpdatedSetResult = getUpdatedSetResultIfExistsOrNull(allSetResults, setResult)

            val loggingWorkoutLift = loggingWorkoutLifts[setResult.liftPosition]
            val setLogEntry = getSetLogEntryFromSetResult(loggingWorkoutLift, setResult, workoutLogEntryId, mesoCycle, microCycle)

            maybeUpdatedSetResult to setLogEntry
        }.let { setResultsAndLogEntries ->
            previousSetResultsRepository.upsertMany(setResultsAndLogEntries.mapNotNull { it.first })
            setLogEntryRepository.upsertMany(setResultsAndLogEntries.map { it.second })
        }
    }
}