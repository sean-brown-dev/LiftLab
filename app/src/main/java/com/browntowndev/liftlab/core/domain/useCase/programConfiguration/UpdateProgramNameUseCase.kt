package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateProgramNameUseCase(
    private val programsRepository: ProgramsRepository
) {
    suspend operator fun invoke(programId: Long, newName: String) {
        programsRepository.updateName(programId, newName)
    }
}