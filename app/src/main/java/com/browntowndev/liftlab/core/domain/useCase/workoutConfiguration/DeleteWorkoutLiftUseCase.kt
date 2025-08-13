package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class DeleteWorkoutLiftUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(programId: Long, workoutLift: GenericWorkoutLift) = transactionScope.execute {
        val delta = programDelta {
            workout(workoutLift.workoutId) {
                removeWorkoutLifts(workoutLift.id)
            }
        }
        programsRepository.applyDelta(programId, delta)
    }
}