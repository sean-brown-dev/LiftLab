package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class SkipDeloadAndStartWorkoutUseCase(
    private val programsRepository: ProgramsRepository,
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programMetadata: ActiveProgramMetadata,
        workoutId: Long,
    ) = transactionScope.execute {
        val delta = programDelta {
            updateProgram(
                currentMesocycle = programMetadata.currentMesocycle + 1,
                currentMicrocycle = 0,
                currentMicrocyclePosition = 0,
            )
        }
        programsRepository.applyDelta(programMetadata.programId, delta)

        startWorkoutUseCase(workoutId = workoutId)
    }
}