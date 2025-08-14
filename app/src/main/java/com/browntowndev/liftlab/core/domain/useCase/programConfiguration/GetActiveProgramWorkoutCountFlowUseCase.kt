package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import kotlinx.coroutines.flow.Flow

class GetActiveProgramWorkoutCountFlowUseCase(private val programsRepository: ProgramsRepository) {
    operator fun invoke(): Flow<Int> {
        return programsRepository.getActiveProgramWorkoutCountFlow()
    }
}