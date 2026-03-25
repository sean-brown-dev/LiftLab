package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateLiftNoteUseCase(
    private val liftRepository: LiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(liftId: Long, note: String) = transactionScope.execute  {
        liftRepository.updateNote(liftId, note.ifEmpty { null })
    }
}