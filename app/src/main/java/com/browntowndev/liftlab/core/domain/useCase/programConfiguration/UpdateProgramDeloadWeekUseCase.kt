package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateProgramDeloadWeekUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(program: Program, deloadWeek: Int, useLiftSpecificDeload: Boolean) = transactionScope.execute {
        val delta = programDelta {
            updateProgram(deloadWeek = deloadWeek)
            program.workouts.getAllLiftsWithRecalculatedStepSize(
                deloadToUseInsteadOfLiftLevel = if (useLiftSpecificDeload) null else deloadWeek,
            ).values.forEach { workoutLiftWithNewSteps ->
                workout(workoutLiftWithNewSteps.workoutId) {
                    lift(workoutLiftId = workoutLiftWithNewSteps.id, stepSize = workoutLiftWithNewSteps.stepSize)
                }
            }
        }

        programsRepository.applyDelta(program.id, delta)
    }
}