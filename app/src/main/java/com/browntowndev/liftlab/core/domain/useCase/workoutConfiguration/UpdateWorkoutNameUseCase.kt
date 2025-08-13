package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateWorkoutNameUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutId: Long,
        newName: String
    ) = transactionScope.execute {
        val delta = programDelta {
            workout(workoutId, name = newName)
        }
        
        programsRepository.applyDelta(programId, delta)
    }
}