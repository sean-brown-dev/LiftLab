package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class SkipDeloadAndStartWorkoutUseCase(
    private val programsRepository: ProgramsRepository,
    private val startWorkoutUseCase: StartWorkoutUseCase,
) {
    suspend operator fun invoke(
        programMetadata: ActiveProgramMetadata,
        workoutId: Long,
    ) {
        programsRepository.updateMesoAndMicroCycle(
            id = programMetadata.programId,
            mesoCycle = programMetadata.currentMesocycle + 1,
            microCycle = 0,
            microCyclePosition = 0,
        )
        startWorkoutUseCase(workoutId = workoutId)
    }
}