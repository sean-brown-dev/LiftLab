package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.useCase.utils.SetResultKey
import com.browntowndev.liftlab.core.domain.useCase.utils.matchesResult

class CompleteSetUseCase(
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        restTime: Long,
        restTimerEnabled: Boolean,
        result: SetResult,
        existingSetResults: List<SetResult>,
        onUpsertSetResult: suspend (SetResult) -> Long
    ) = transactionScope.execute {
        if (restTimerEnabled) {
            restTimerInProgressRepository.insert(restTime)
        }

        // We do not know the ID of this result, so have to search by values
        val setResultKey = SetResultKey(
            liftId = result.liftId,
            liftPosition = result.liftPosition,
            setPosition = result.setPosition,
            myoRepSetPosition = (result as? MyoRepSetResult)?.myoRepSetPosition,
        )
        val resultToUpsert = existingSetResults
            .firstOrNull { setResultKey.matchesResult(it) }
            ?.let { existingResult -> result.copyBase(id = existingResult.id) }
            ?: result
        onUpsertSetResult(resultToUpsert)
    }
}