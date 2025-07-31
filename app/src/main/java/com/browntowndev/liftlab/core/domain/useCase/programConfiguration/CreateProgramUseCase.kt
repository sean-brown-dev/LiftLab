package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class CreateProgramUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(name: String, isActive: Boolean, currentActiveProgram: Program?) = transactionScope.execute {
        programsRepository.insert(
            Program(
                name = name,
                isActive = isActive,
            )
        )

        if (isActive && currentActiveProgram != null) {
            currentActiveProgram.copy(isActive = false)
            programsRepository.update(currentActiveProgram)
        }
    }
}