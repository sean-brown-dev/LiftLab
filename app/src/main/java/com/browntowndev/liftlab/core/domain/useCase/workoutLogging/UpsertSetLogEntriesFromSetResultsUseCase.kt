package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository

class UpsertSetLogEntriesFromSetResultsUseCase(
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
): BaseUpsertSetLogEntryUseCase() {
    suspend operator fun invoke(
        workoutLogEntryId: Long,
        loggingWorkoutLifts: List<LoggingWorkoutLift>,
        setResults: List<SetResult>,
    ): List<Long>  = transactionScope.executeWithResult {
        setResults.fastMap { setResult ->
            if (setResult.liftPosition >= loggingWorkoutLifts.size) throw Exception("Lift position is out of bounds")
            val loggingWorkoutLift = loggingWorkoutLifts[setResult.liftPosition]
            getSetLogEntryFromSetResult(loggingWorkoutLift, setResult, workoutLogEntryId)
        }.let { setLogEntries ->
            setLogEntryRepository.upsertMany(setLogEntries)
        }
    }
}