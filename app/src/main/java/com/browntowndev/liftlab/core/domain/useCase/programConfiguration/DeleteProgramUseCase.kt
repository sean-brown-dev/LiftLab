package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class DeleteProgramUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(programId: Long) = transactionScope.execute {
        programsRepository.deleteById(programId)
        val allPrograms = programsRepository.getAll()
        if (!allPrograms.isEmpty() && !allPrograms.any { it.isActive }) {
            val newActiveProgram = allPrograms.first().copy(isActive = true)
            programsRepository.update(newActiveProgram)
        }
    }
}