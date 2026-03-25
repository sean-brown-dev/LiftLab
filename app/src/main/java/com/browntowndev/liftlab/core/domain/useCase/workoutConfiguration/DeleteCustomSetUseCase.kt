package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class DeleteCustomSetUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(programId: Long, workoutId: Long, workoutLiftId: Long, setId: Long) = transactionScope.execute {
        val delta = programDelta {
            workout(workoutId) {
                updateSets(workoutLiftId) {
                    removeSets(setId)
                }
            }
        }
        programsRepository.applyDelta(programId, delta)
    }
}