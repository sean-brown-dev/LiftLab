package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class DeleteProgramUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(programId: Long) = transactionScope.execute {
        val delta = programDelta {
            deleteProgram()
        }
        programsRepository.applyDelta(programId, delta)

        val activeProgram = programsRepository.getActive()
        if (activeProgram == null) {
            programsRepository.getNewest()?.copy(isActive = true)?.let { newActiveProgram ->
                val activeProgramDelta = programDelta {
                    updateProgram(isActive = Patch.Set(true))
                }
                programsRepository.applyDelta(newActiveProgram.id, activeProgramDelta)
            }
        }
    }
}
