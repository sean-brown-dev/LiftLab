package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.Patch
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
            updateProgram(deloadWeek = Patch.Set(deloadWeek))
            program.workouts.getAllLiftsWithRecalculatedStepSize(
                deloadToUseInsteadOfLiftLevel = if (useLiftSpecificDeload) null else deloadWeek,
            ).forEach { workoutLiftWithNewSteps ->
                val workoutId = workoutLiftWithNewSteps.key
                val liftsWithNewSteps = workoutLiftWithNewSteps.value
                workout(workoutId) {
                    liftsWithNewSteps.fastForEach { workoutLift ->
                        updateLift(workoutLiftId = workoutLift.id, stepSize = Patch.Set(workoutLift.stepSize))
                    }
                }
            }
        }

        programsRepository.applyDelta(program.id, delta)
    }
}