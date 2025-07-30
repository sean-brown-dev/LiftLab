package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository

class UpdateLiftNoteUseCase(
    private val liftRepository: LiftsRepository,
) {
    suspend operator fun invoke(liftId: Long, note: String) {
        liftRepository.updateNote(liftId, note.ifEmpty { null })
    }
}