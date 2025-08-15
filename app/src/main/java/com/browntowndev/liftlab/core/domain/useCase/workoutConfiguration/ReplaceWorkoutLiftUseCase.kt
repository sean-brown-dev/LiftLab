package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class ReplaceWorkoutLiftUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        workoutId: Long,
        workoutLiftId: Long,
        replacementLiftId: Long
    ) = transactionScope.execute {
        val programId = programsRepository.getForWorkout(workoutId)?.id ?: error("Program not found for workout: $workoutId")
        val delta = programDelta {
            workout(workoutId) {
                updateLift(workoutLiftId, liftId = Patch.Set(replacementLiftId))
            }
        }

        programsRepository.applyDelta(programId, delta)
    }
}