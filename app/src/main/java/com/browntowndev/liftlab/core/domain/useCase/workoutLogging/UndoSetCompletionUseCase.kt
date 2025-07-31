package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult

class UndoSetCompletionUseCase(
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?,
        setResults: List<SetResult>,
        onDeleteSetResult: suspend (id: Long) -> Unit,
    ) = transactionScope.execute {
        setResults
            .find {
                it.liftPosition == liftPosition &&
                        it.setPosition == setPosition &&
                        (it as? MyoRepSetResult)?.myoRepSetPosition == myoRepSetPosition
            }?.let { result ->
                onDeleteSetResult(result.id)
            }
    }
}