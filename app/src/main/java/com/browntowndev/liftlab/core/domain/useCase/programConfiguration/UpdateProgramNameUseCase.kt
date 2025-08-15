package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateProgramNameUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(programId: Long, newName: String) = transactionScope.execute {
        val delta = programDelta {
            updateProgram(name = Patch.Set(newName))
        }
        programsRepository.applyDelta(programId, delta)
    }
}