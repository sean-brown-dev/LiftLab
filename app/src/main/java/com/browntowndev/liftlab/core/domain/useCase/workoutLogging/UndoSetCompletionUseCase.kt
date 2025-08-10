package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository

class UndoSetCompletionUseCase(
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?,
        setResults: List<SetResult>,
        onDeleteSetResult: suspend (id: Long) -> Unit,
    ) = transactionScope.execute {
        val setResultToDelete = setResults
            .find {
                it.liftPosition == liftPosition &&
                        it.setPosition == setPosition &&
                        (it as? MyoRepSetResult)?.myoRepSetPosition == myoRepSetPosition
            } ?: return@execute

        if (workoutInProgressRepository.isWorkoutInProgress(setResultToDelete.workoutId)) {
            restTimerInProgressRepository.deleteAll()
        }

        onDeleteSetResult(setResultToDelete.id)
    }
}