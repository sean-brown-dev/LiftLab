package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateCustomLiftSetUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutId: Long,
        set: GenericLiftSet
    ) = transactionScope.execute {
        val delta = programDelta {
            workout(workoutId) {
                updateSets(set.workoutLiftId) {
                    set(set)
                }
            }
        }
        programsRepository.applyDelta(programId, delta)
    }
}